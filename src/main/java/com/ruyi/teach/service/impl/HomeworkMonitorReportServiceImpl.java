package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.HomeworkMonitorReportMapper;
import com.ruyi.teach.model.entity.HomeworkMonitorReport;
import com.ruyi.teach.service.HomeworkMonitorReportService;
import org.springframework.stereotype.Service;

@Service
public class HomeworkMonitorReportServiceImpl
        extends ServiceImpl<HomeworkMonitorReportMapper, HomeworkMonitorReport>
        implements HomeworkMonitorReportService {
}