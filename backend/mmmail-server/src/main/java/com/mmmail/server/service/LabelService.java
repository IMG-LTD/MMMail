package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailLabelMapper;
import com.mmmail.server.model.entity.MailLabel;
import com.mmmail.server.model.vo.LabelVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LabelService {

    private final MailLabelMapper mailLabelMapper;
    private final MailFilterService mailFilterService;

    public LabelService(MailLabelMapper mailLabelMapper, MailFilterService mailFilterService) {
        this.mailLabelMapper = mailLabelMapper;
        this.mailFilterService = mailFilterService;
    }

    public List<LabelVo> list(Long userId) {
        return mailLabelMapper.selectList(new LambdaQueryWrapper<MailLabel>()
                        .eq(MailLabel::getOwnerId, userId)
                        .orderByAsc(MailLabel::getName))
                .stream()
                .map(label -> new LabelVo(label.getId(), label.getName(), label.getColor()))
                .toList();
    }

    @Transactional
    public void create(Long userId, String name, String color) {
        MailLabel exists = mailLabelMapper.selectOne(new LambdaQueryWrapper<MailLabel>()
                .eq(MailLabel::getOwnerId, userId)
                .eq(MailLabel::getName, name));
        if (exists != null) {
            throw new BizException(ErrorCode.LABEL_ALREADY_EXISTS);
        }

        MailLabel label = new MailLabel();
        label.setOwnerId(userId);
        label.setName(name);
        label.setColor(color);
        label.setCreatedAt(LocalDateTime.now());
        label.setUpdatedAt(LocalDateTime.now());
        label.setDeleted(0);
        mailLabelMapper.insert(label);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MailLabel label = mailLabelMapper.selectOne(new LambdaQueryWrapper<MailLabel>()
                .eq(MailLabel::getId, id)
                .eq(MailLabel::getOwnerId, userId));
        if (label == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Label does not exist");
        }
        mailFilterService.assertLabelNotUsed(userId, label.getName());
        mailLabelMapper.deleteById(id);
    }
}
