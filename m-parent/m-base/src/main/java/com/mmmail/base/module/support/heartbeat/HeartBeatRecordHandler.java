package com.mmmail.base.module.support.heartbeat;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.mmmail.base.common.util.SmartBeanUtil;
import com.mmmail.base.module.support.heartbeat.core.HeartBeatRecord;
import com.mmmail.base.module.support.heartbeat.core.IHeartBeatRecordHandler;
import com.mmmail.base.module.support.heartbeat.domain.HeartBeatRecordEntity;
import org.springframework.stereotype.Service;

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
public class HeartBeatRecordHandler implements IHeartBeatRecordHandler {

    @Resource
    private HeartBeatRecordDao heartBeatRecordDao;

    /**
     * 心跳日志处理方法
     * @param heartBeatRecord
     */
    @Override
    public void handler(HeartBeatRecord heartBeatRecord) {
        HeartBeatRecordEntity heartBeatRecordEntity = SmartBeanUtil.copy(heartBeatRecord, HeartBeatRecordEntity.class);
        HeartBeatRecordEntity heartBeatRecordOld = heartBeatRecordDao.query(heartBeatRecordEntity);
        if (heartBeatRecordOld == null) {
            heartBeatRecordDao.insert(heartBeatRecordEntity);
        } else {
            heartBeatRecordDao.updateHeartBeatTimeById(heartBeatRecordOld.getHeartBeatRecordId(), heartBeatRecordEntity.getHeartBeatTime());
        }
    }

}
