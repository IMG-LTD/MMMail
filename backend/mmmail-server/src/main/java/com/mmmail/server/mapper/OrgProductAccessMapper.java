package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.OrgProductAccess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OrgProductAccessMapper extends BaseMapper<OrgProductAccess> {

    @Select("""
            SELECT id, org_id, member_id, product_key, access_state, updated_by, created_at, updated_at, deleted
            FROM org_product_access
            WHERE org_id = #{orgId}
              AND member_id = #{memberId}
              AND product_key = #{productKey}
            LIMIT 1
            """)
    OrgProductAccess selectIncludingDeleted(
            @Param("orgId") Long orgId,
            @Param("memberId") Long memberId,
            @Param("productKey") String productKey
    );

    @Update("""
            UPDATE org_product_access
            SET access_state = #{accessState},
                updated_by = #{updatedBy},
                updated_at = #{updatedAt},
                deleted = 0
            WHERE id = #{id}
            """)
    int restoreDisabledAccess(
            @Param("id") Long id,
            @Param("accessState") String accessState,
            @Param("updatedBy") Long updatedBy,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
