package com.mmmail.server.jobs;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlatformJobRunMapper extends BaseMapper<PlatformJobRun> {

    @Select("""
            select *
            from platform_job_run
            where idempotency_key = #{idempotencyKey}
            limit 1
            """)
    PlatformJobRun findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            select *
            from platform_job_run
            where status in ('QUEUED', 'RETRYABLE')
              and (next_attempt_at is null or next_attempt_at <= #{now})
            order by created_at, id
            limit #{limit}
            """)
    List<PlatformJobRun> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
