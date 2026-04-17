package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.MailMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {

    @Select("""
            <script>
            WITH filtered_messages AS (
                SELECT id,
                       direction,
                       sent_at,
                       subject,
                       CASE
                           WHEN peer_email IS NOT NULL AND TRIM(peer_email) != '' THEN CONCAT('email:', LOWER(TRIM(peer_email)))
                           WHEN peer_id IS NOT NULL THEN CONCAT('peer-id:', peer_id)
                           WHEN direction = 'IN' AND sender_email IS NOT NULL AND TRIM(sender_email) != '' THEN CONCAT('email:', LOWER(TRIM(sender_email)))
                           ELSE CONCAT('message-id:', id)
                       END AS participant_key
                FROM mail_message
                WHERE owner_id = #{ownerId}
                  AND is_draft = 0
                  AND deleted = 0
                  AND folder_type NOT IN ('OUTBOX', 'SCHEDULED')
            ),
            participant_scoped_messages AS (
                SELECT id,
                       direction,
                       sent_at,
                       subject,
                       participant_key
                FROM filtered_messages
                WHERE participant_key IN
                    <foreach collection="participantKeys" item="participantKey" open="(" separator="," close=")">
                        #{participantKey}
                    </foreach>
            ),
            matching_messages AS (
                SELECT id,
                       direction,
                       sent_at,
                       participant_key,
                       CASE
                           WHEN subject IS NULL OR TRIM(subject) = '' THEN #{noSubjectKey}
                           WHEN TRIM(REGEXP_REPLACE(LOWER(TRIM(subject)), '^((re:|fwd:)\\s*)+', '')) = '' THEN #{noSubjectKey}
                           ELSE TRIM(REGEXP_REPLACE(LOWER(TRIM(subject)), '^((re:|fwd:)\\s*)+', ''))
                       END AS conversation_key
                FROM participant_scoped_messages
            ),
            requested_messages AS (
                SELECT *
                FROM matching_messages
                WHERE conversation_key IN
                    <foreach collection="conversationKeys" item="conversationKey" open="(" separator="," close=")">
                        #{conversationKey}
                    </foreach>
            ),
            aggregated_messages AS (
                SELECT conversation_key,
                       participant_key,
                       COUNT(*) AS message_count,
                       MAX(CASE WHEN direction = 'IN' THEN 1 ELSE 0 END) AS has_inbound,
                       MAX(CASE WHEN direction = 'OUT' THEN 1 ELSE 0 END) AS has_outbound
                FROM requested_messages
                GROUP BY conversation_key, participant_key
            ),
            ranked_messages AS (
                SELECT conversation_key,
                       participant_key,
                       direction,
                       ROW_NUMBER() OVER (PARTITION BY conversation_key, participant_key ORDER BY sent_at DESC, id DESC) AS row_num
                FROM requested_messages
            )
            SELECT aggregated_messages.conversation_key AS conversationKey,
                   aggregated_messages.participant_key AS participantKey,
                   aggregated_messages.message_count AS messageCount,
                   aggregated_messages.has_inbound AS hasInbound,
                   aggregated_messages.has_outbound AS hasOutbound,
                   ranked_messages.direction AS latestDirection
            FROM aggregated_messages
            JOIN ranked_messages
              ON ranked_messages.conversation_key = aggregated_messages.conversation_key
             AND ranked_messages.participant_key = aggregated_messages.participant_key
             AND ranked_messages.row_num = 1
            </script>
            """)
    List<ConversationTriageAggregateRow> selectConversationTriageAggregates(
            @Param("ownerId") Long ownerId,
            @Param("noSubjectKey") String noSubjectKey,
            @Param("conversationKeys") Collection<String> conversationKeys,
            @Param("participantKeys") Collection<String> participantKeys
    );

    class ConversationTriageAggregateRow {
        private String conversationKey;
        private String participantKey;
        private Integer messageCount;
        private Integer hasInbound;
        private Integer hasOutbound;
        private String latestDirection;

        public String getConversationKey() {
            return conversationKey;
        }

        public void setConversationKey(String conversationKey) {
            this.conversationKey = conversationKey;
        }

        public String getParticipantKey() {
            return participantKey;
        }

        public void setParticipantKey(String participantKey) {
            this.participantKey = participantKey;
        }

        public Integer getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(Integer messageCount) {
            this.messageCount = messageCount;
        }

        public Integer getHasInbound() {
            return hasInbound;
        }

        public void setHasInbound(Integer hasInbound) {
            this.hasInbound = hasInbound;
        }

        public Integer getHasOutbound() {
            return hasOutbound;
        }

        public void setHasOutbound(Integer hasOutbound) {
            this.hasOutbound = hasOutbound;
        }

        public String getLatestDirection() {
            return latestDirection;
        }

        public void setLatestDirection(String latestDirection) {
            this.latestDirection = latestDirection;
        }
    }
}
