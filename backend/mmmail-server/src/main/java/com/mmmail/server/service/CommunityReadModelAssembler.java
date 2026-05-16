package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.model.entity.CommunityComment;
import com.mmmail.server.model.entity.CommunityPost;
import com.mmmail.server.model.entity.CommunityReport;
import com.mmmail.server.model.entity.CommunityTopic;
import com.mmmail.server.model.vo.CommunityCommentVo;
import com.mmmail.server.model.vo.CommunityPostPageVo;
import com.mmmail.server.model.vo.CommunityPostVo;
import com.mmmail.server.model.vo.CommunityReportVo;
import com.mmmail.server.model.vo.CommunityTopicVo;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CommunityReadModelAssembler {

    private static final int MAX_TAGS = 8;
    private static final int MAX_TAG_LENGTH = 32;
    private static final Safelist COMMUNITY_HTML_SAFELIST = Safelist.basic()
            .addTags("h1")
            .removeTags("img");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CommunityReadModelAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CommunityPostPageVo toPostPage(IPage<CommunityPost> result, int page, int size) {
        return new CommunityPostPageVo(
                result.getRecords().stream().map(this::toPostVo).toList(),
                result.getTotal(),
                page,
                size
        );
    }

    public CommunityTopicVo toTopicVo(CommunityTopic topic) {
        return new CommunityTopicVo(
                topic.getId(),
                topic.getSlug(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getSortOrder(),
                topic.getCreatedAt()
        );
    }

    public CommunityPostVo toPostVo(CommunityPost post) {
        return new CommunityPostVo(
                post.getId(),
                post.getAuthorUserId(),
                post.getTopicId(),
                post.getTitle(),
                post.getBodyMd(),
                post.getBodyHtml(),
                readTags(post.getTagsJson()),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                post.getPinned() == 1,
                post.getLocked() == 1,
                post.getStatus(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public List<CommunityCommentVo> buildCommentTree(List<CommunityComment> comments) {
        Map<String, List<CommunityComment>> repliesByParent = new LinkedHashMap<>();
        List<CommunityComment> roots = new ArrayList<>();
        for (CommunityComment comment : comments) {
            if (comment.getParentCommentId() == null) {
                roots.add(comment);
                continue;
            }
            repliesByParent.computeIfAbsent(comment.getParentCommentId(), ignored -> new ArrayList<>()).add(comment);
        }
        return roots.stream().map(root -> toCommentVo(root, repliesByParent.getOrDefault(root.getId(), List.of()))).toList();
    }

    public CommunityCommentVo toCommentVo(CommunityComment comment, List<CommunityComment> replies) {
        return new CommunityCommentVo(
                comment.getId(),
                comment.getPostId(),
                comment.getParentCommentId(),
                comment.getAuthorUserId(),
                comment.getBodyMd(),
                comment.getBodyHtml(),
                comment.getStatus(),
                comment.getCreatedAt(),
                replies.stream().map(reply -> toCommentVo(reply, List.of())).toList()
        );
    }

    public CommunityReportVo toReportVo(CommunityReport report) {
        return new CommunityReportVo(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReporterUserId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getAssigneeUserId(),
                report.getAction(),
                report.getActionNote(),
                report.getCreatedAt(),
                report.getActionedAt()
        );
    }

    public List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = StringUtils.hasText(tag) ? tag.trim() : null;
            if (value != null) {
                normalized.add(value.substring(0, Math.min(value.length(), MAX_TAG_LENGTH)));
            }
        }
        return normalized.stream().limit(MAX_TAGS).toList();
    }

    public String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize community tags", exception);
        }
    }

    public List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize community tags", exception);
        }
    }

    public String renderMarkdown(String markdown) {
        StringBuilder html = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            appendMarkdownLine(html, line);
        }
        return Jsoup.clean(html.toString(), COMMUNITY_HTML_SAFELIST);
    }

    private void appendMarkdownLine(StringBuilder html, String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("# ")) {
            String heading = sanitizeMarkdownText(trimmed.substring(2));
            if (StringUtils.hasText(heading)) {
                html.append("<h1>").append(escapeHtml(heading)).append("</h1>");
            }
            return;
        }
        String paragraph = sanitizeMarkdownText(trimmed);
        if (StringUtils.hasText(paragraph)) {
            html.append("<p>").append(escapeHtml(paragraph)).append("</p>");
        }
    }

    private String sanitizeMarkdownText(String value) {
        return Jsoup.parse(value).text();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
