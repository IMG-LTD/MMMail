package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.CalendarSubscriptionEvent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CalendarSubscriptionEventMapper extends BaseMapper<CalendarSubscriptionEvent> {

    @Select("""
            select event_id
            from calendar_subscription_event
            where owner_id = #{ownerId}
              and subscription_id = #{subscriptionId}
            """)
    List<Long> selectEventIds(Long ownerId, Long subscriptionId);

    @Delete("""
            delete from calendar_subscription_event
            where owner_id = #{ownerId}
              and subscription_id = #{subscriptionId}
            """)
    int deleteBySubscription(Long ownerId, Long subscriptionId);
}
