package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.CalendarEventShare;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalendarEventShareMapper extends BaseMapper<CalendarEventShare> {

    @Delete("delete from calendar_event_share where event_id = #{eventId} and target_user_id = #{targetUserId}")
    int purgeByEventAndTarget(@Param("eventId") Long eventId, @Param("targetUserId") Long targetUserId);

    @Delete("delete from calendar_event_share where owner_id = #{ownerId} and event_id = #{eventId} and id = #{shareId}")
    int purgeByOwnerEventAndId(@Param("ownerId") Long ownerId, @Param("eventId") Long eventId, @Param("shareId") Long shareId);

    @Delete("delete from calendar_event_share where event_id = #{eventId}")
    int purgeByEventId(@Param("eventId") Long eventId);
}
