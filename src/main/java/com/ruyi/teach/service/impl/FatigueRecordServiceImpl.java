package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.FatigueRecordMapper;
import com.ruyi.teach.model.entity.FatigueRecord;
import com.ruyi.teach.service.FatigueRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FatigueRecordServiceImpl extends ServiceImpl<FatigueRecordMapper, FatigueRecord>
        implements FatigueRecordService {

    @Override
    public FatigueRecord getByUserAndDate(Long userId, LocalDate date) {
        return getOne(new QueryWrapper<FatigueRecord>()
                .eq("user_id", userId)
                .eq("record_date", date));
    }

    @Override
    public FatigueRecord saveOrUpdateToday(Long userId, FatigueRecord incoming) {
        LocalDate today = LocalDate.now();

        // 查询今天是否已有记录
        FatigueRecord existing = getByUserAndDate(userId, today);

        if (existing == null) {
            // 首次上报：直接插入
            incoming.setUserId(userId);
            incoming.setRecordDate(today);
            save(incoming);
            return incoming;
        } else {
            // 后续上报：用前端传来的最新全量数据覆盖
            existing.setCourseId(incoming.getCourseId());
            existing.setChapterId(incoming.getChapterId());
            existing.setYawnCount(incoming.getYawnCount());
            existing.setFatigueCount(incoming.getFatigueCount());
            existing.setNoFaceCount(incoming.getNoFaceCount());
            existing.setNormalCount(incoming.getNormalCount());
            existing.setTotalDetections(incoming.getTotalDetections());
            existing.setMonitorSeconds(incoming.getMonitorSeconds());
            existing.setEvents(incoming.getEvents());
            existing.setEarSamples(incoming.getEarSamples());
            existing.setMarSamples(incoming.getMarSamples());
            existing.setLastStatus(incoming.getLastStatus());
            updateById(existing);
            return existing;
        }
    }
}
