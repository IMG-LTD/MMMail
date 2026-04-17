package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("""
            <script>
            SELECT *
            FROM user_account
            WHERE deleted = 0
              AND LOWER(email) IN
                <foreach collection="normalizedEmails" item="email" open="(" separator="," close=")">
                    #{email}
                </foreach>
            </script>
            """)
    List<UserAccount> selectActiveByNormalizedEmails(@Param("normalizedEmails") Collection<String> normalizedEmails);
}
