package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.TeacherScheduleMapper;
import com.ruyi.teach.model.entity.TeacherSchedule;
import com.ruyi.teach.service.TeacherScheduleService;
import org.springframework.stereotype.Service;

@Service
public class TeacherScheduleServiceImpl extends ServiceImpl<TeacherScheduleMapper, TeacherSchedule> implements TeacherScheduleService {
}
