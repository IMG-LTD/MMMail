package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthenticatorEntryMapper extends BaseMapper<AuthenticatorEntry> {
}
