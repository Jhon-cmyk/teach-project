package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.CodingProblemTemplateMapper;
import com.ruyi.teach.model.entity.CodingProblemTemplate;
import com.ruyi.teach.service.CodingProblemTemplateService;
import org.springframework.stereotype.Service;

@Service
public class CodingProblemTemplateServiceImpl extends ServiceImpl<CodingProblemTemplateMapper, CodingProblemTemplate>
        implements CodingProblemTemplateService {
}
