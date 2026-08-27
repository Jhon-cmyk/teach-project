package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.MentalStateRecordMapper;
import com.ruyi.teach.model.entity.MentalStateRecord;
import com.ruyi.teach.service.MentalStateRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MentalStateRecordServiceImpl
        extends ServiceImpl<MentalStateRecordMapper, MentalStateRecord>
        implements MentalStateRecordService {

    @Override
    public Long saveRecord(MentalStateRecord record) {
        this.save(record);
        return record.getId();
    }

    @Override
    public List<MentalStateRecord> listByUser(Long userId, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
        QueryWrapper<MentalStateRecord> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
          .orderByDesc("create_time")
          .last("LIMIT " + safeLimit);
        return this.list(qw);
    }
}