package com.mmmail.base.module.support.changelog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.mmmail.base.common.domain.PageResult;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.util.SmartBeanUtil;
import com.mmmail.base.common.util.SmartPageUtil;
import com.mmmail.base.module.support.changelog.dao.ChangeLogDao;
import com.mmmail.base.module.support.changelog.domain.entity.ChangeLogEntity;
import com.mmmail.base.module.support.changelog.domain.form.ChangeLogAddForm;
import com.mmmail.base.module.support.changelog.domain.form.ChangeLogQueryForm;
import com.mmmail.base.module.support.changelog.domain.form.ChangeLogUpdateForm;
import com.mmmail.base.module.support.changelog.domain.vo.ChangeLogVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统更新日志 Service
 *
 * @Author 卓大
 * @Date 2022-09-26 14:53:50
 * @Copyright 1024创新实验室
 */

@Service
public class ChangeLogService {

    @Resource
    private ChangeLogDao changeLogDao;

    /**
     * 分页查询
     */
    public PageResult<ChangeLogVO> queryPage(ChangeLogQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<ChangeLogVO> list = changeLogDao.queryPage(page, queryForm);
        return SmartPageUtil.convert2PageResult(page, list);
    }

    /**
     * 添加
     */
    public synchronized ResponseDTO<String> add(ChangeLogAddForm addForm) {
        ChangeLogEntity existVersion = changeLogDao.selectByVersion(addForm.getUpdateVersion());
        if (existVersion != null) {
            return ResponseDTO.userErrorParam("此版本已经存在");
        }

        ChangeLogEntity changeLogEntity = SmartBeanUtil.copy(addForm, ChangeLogEntity.class);
        changeLogDao.insert(changeLogEntity);
        return ResponseDTO.ok();
    }

    /**
     * 更新
     */
    public synchronized ResponseDTO<String> update(ChangeLogUpdateForm updateForm) {
        ChangeLogEntity existVersion = changeLogDao.selectByVersion(updateForm.getUpdateVersion());
        if (existVersion != null && !updateForm.getChangeLogId().equals(existVersion.getChangeLogId())) {
            return ResponseDTO.userErrorParam("此版本已经存在");
        }
        ChangeLogEntity changeLogEntity = SmartBeanUtil.copy(updateForm, ChangeLogEntity.class);
        changeLogDao.updateById(changeLogEntity);
        return ResponseDTO.ok();
    }

    /**
     * 批量删除
     */
    public synchronized ResponseDTO<String> batchDelete(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return ResponseDTO.ok();
        }

        changeLogDao.deleteBatchIds(idList);
        return ResponseDTO.ok();
    }

    /**
     * 单个删除
     */
    public synchronized ResponseDTO<String> delete(Long changeLogId) {
        if (null == changeLogId) {
            return ResponseDTO.ok();
        }

        changeLogDao.deleteById(changeLogId);
        return ResponseDTO.ok();
    }

    public ChangeLogVO getById(Long changeLogId) {
        return SmartBeanUtil.copy(changeLogDao.selectById(changeLogId), ChangeLogVO.class);
    }
}
