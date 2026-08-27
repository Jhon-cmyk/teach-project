package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.CodingProblemMapper;
import com.ruyi.teach.model.entity.CodingProblem;
import com.ruyi.teach.service.CodingProblemService;
import org.springframework.stereotype.Service;

@Service
public class CodingProblemServiceImpl extends ServiceImpl<CodingProblemMapper, CodingProblem>
        implements CodingProblemService {
}
