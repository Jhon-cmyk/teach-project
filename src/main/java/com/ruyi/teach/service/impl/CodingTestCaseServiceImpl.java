package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.CodingTestCaseMapper;
import com.ruyi.teach.model.entity.CodingTestCase;
import com.ruyi.teach.service.CodingTestCaseService;
import org.springframework.stereotype.Service;

@Service
public class CodingTestCaseServiceImpl extends ServiceImpl<CodingTestCaseMapper, CodingTestCase>
        implements CodingTestCaseService {
}
