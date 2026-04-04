package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.CalendarEventAttendee;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalendarEventAttendeeMapper extends BaseMapper<CalendarEventAttendee> {

    @Delete("delete from calendar_event_attendee where owner_id = #{ownerId} and event_id = #{eventId}")
    int purgeByOwnerAndEvent(@Param("ownerId") Long ownerId, @Param("eventId") Long eventId);

    @Delete("delete from calendar_event_attendee where event_id = #{eventId}")
    int purgeByEventId(@Param("eventId") Long eventId);

    @Delete("delete from calendar_event_attendee where event_id = #{eventId} and email = #{email}")
    int purgeByEventAndEmail(@Param("eventId") Long eventId, @Param("email") String email);
}
