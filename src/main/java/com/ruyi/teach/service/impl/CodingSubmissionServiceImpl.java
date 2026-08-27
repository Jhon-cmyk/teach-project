package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.CodingSubmissionMapper;
import com.ruyi.teach.model.entity.CodingSubmission;
import com.ruyi.teach.service.CodingSubmissionService;
import org.springframework.stereotype.Service;

@Service
public class CodingSubmissionServiceImpl extends ServiceImpl<CodingSubmissionMapper, CodingSubmission>
        implements CodingSubmissionService {
}
