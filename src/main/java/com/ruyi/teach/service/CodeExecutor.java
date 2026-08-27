package com.ruyi.teach.service;

import com.ruyi.teach.model.vo.CodingRunResultVO;

public interface CodeExecutor {
    CodingRunResultVO execute(String language, String code, String stdin,
                              Long timeoutMs, Long memoryLimitKb);
}
