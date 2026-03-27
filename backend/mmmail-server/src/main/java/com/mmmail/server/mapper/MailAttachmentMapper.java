package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.MailAttachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailAttachmentMapper extends BaseMapper<MailAttachment> {
}
