package com.mmmail.base.module.support.job.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmmail.base.module.support.job.api.domain.SmartJobQueryForm;
import com.mmmail.base.module.support.job.api.domain.SmartJobVO;
import com.mmmail.base.module.support.job.repository.domain.SmartJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 定时任务 dao
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Mapper
public interface SmartJobDao extends BaseMapper<SmartJobEntity> {

    /**
     * 定时任务-分页查询
     *
     * @param page
     * @param queryForm
     * @return
     */
    List<SmartJobVO> query(Page<?> page, @Param("query") SmartJobQueryForm queryForm);

    /**
     * 假删除
     *
     * @param jobId
     * @return
     */
    void updateDeletedFlag(@Param("jobId") Integer jobId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 根据 任务class 查找
     *
     * @param jobClass
     * @return
     */
    SmartJobEntity selectByJobClass(@Param("jobClass") String jobClass);
}
