package com.mmmail.base.module.support.heartbeat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.mmmail.base.common.domain.PageResult;
import com.mmmail.base.common.domain.ResponseDTO;
import com.mmmail.base.common.util.SmartPageUtil;
import com.mmmail.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import com.mmmail.base.module.support.heartbeat.domain.HeartBeatRecordVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 心跳记录
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-01-09 20:57:24
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class HeartBeatService {

    @Resource
    private HeartBeatRecordDao heartBeatRecordDao;

    public ResponseDTO<PageResult<HeartBeatRecordVO>> pageQuery(HeartBeatRecordQueryForm pageParam) {
        Page pageQueryInfo = SmartPageUtil.convert2PageQuery(pageParam);
        List<HeartBeatRecordVO> recordVOList = heartBeatRecordDao.pageQuery(pageQueryInfo,pageParam);
        PageResult<HeartBeatRecordVO> pageResult = SmartPageUtil.convert2PageResult(pageQueryInfo, recordVOList);
        return ResponseDTO.ok(pageResult);
    }
}
