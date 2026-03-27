package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.MailLabel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailLabelMapper extends BaseMapper<MailLabel> {
}
