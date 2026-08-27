package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.FatigueRecord;

import java.time.LocalDate;

public interface FatigueRecordService extends IService<FatigueRecord> {

    /**
     * 获取某用户某天的疲劳记录（没有则返回 null）
     */
    FatigueRecord getByUserAndDate(Long userId, LocalDate date);

    /**
     * 上报/更新今日疲劳数据（前端每隔一段时间调一次，做增量合并）
     */
    FatigueRecord saveOrUpdateToday(Long userId, FatigueRecord incoming);
}