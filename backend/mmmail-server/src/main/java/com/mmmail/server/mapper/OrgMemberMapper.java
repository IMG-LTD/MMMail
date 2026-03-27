package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.OrgMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OrgMemberMapper extends BaseMapper<OrgMember> {

    @Select("""
            SELECT id, org_id, user_id, user_email, role, status, invited_by, joined_at, created_at, updated_at, deleted
            FROM org_member
            WHERE org_id = #{orgId}
              AND user_email = #{userEmail}
            ORDER BY deleted ASC, updated_at DESC
            LIMIT 1
            """)
    OrgMember selectAnyByOrgAndEmail(@Param("orgId") Long orgId, @Param("userEmail") String userEmail);

    @Update("""
            UPDATE org_member
            SET user_id = #{userId},
                user_email = #{userEmail},
                role = #{role},
                status = #{status},
                invited_by = #{invitedBy},
                joined_at = #{joinedAt},
                updated_at = #{updatedAt},
                deleted = #{deleted}
            WHERE id = #{id}
            """)
    int updateIncludingDeleted(OrgMember member);
}
