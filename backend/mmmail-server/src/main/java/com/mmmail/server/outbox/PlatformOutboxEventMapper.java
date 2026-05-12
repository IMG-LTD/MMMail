package com.mmmail.server.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlatformOutboxEventMapper extends BaseMapper<PlatformOutboxEvent> {

    @Select("""
            select *
            from platform_outbox_event
            where idempotency_key = #{idempotencyKey}
            limit 1
            """)
    PlatformOutboxEvent findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            select *
            from platform_outbox_event
            where status in ('PENDING', 'FAILED')
              and (next_attempt_at is null or next_attempt_at <= #{now})
            order by created_at, id
            limit #{limit}
            """)
    List<PlatformOutboxEvent> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
