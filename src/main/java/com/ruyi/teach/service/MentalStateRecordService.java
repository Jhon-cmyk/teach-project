package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.MentalStateRecord;

import java.util.List;

public interface MentalStateRecordService extends IService<MentalStateRecord> {

    /**
     * 保存一次认知状态评估记录
     */
    Long saveRecord(MentalStateRecord record);

    /**
     * 查询某用户最近的评估记录(按时间倒序)
     */
    List<MentalStateRecord> listByUser(Long userId, Integer limit);
}