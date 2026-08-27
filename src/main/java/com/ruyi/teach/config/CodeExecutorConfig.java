package com.ruyi.teach.config;

import com.ruyi.teach.service.CodeExecutor;
import com.ruyi.teach.client.Judge0Client;
import com.ruyi.teach.service.LocalCodeExecutor;
import com.ruyi.teach.model.vo.CodingRunResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeExecutorConfig {

    @Value("${judge0.default-timeout-ms:5000}")
    private long defaultTimeoutMs;

    @Value("${code-executor.mode:local}")
    private String mode;

    @Bean
    public CodeExecutor codeExecutor(Judge0Client judge0Client) {
        if ("judge0".equalsIgnoreCase(mode)) {
            return (language, code, stdin, timeoutMs, memoryLimitKb) -> {
                Judge0Client.JudgeResult jr = judge0Client.submitAndWait(
                        language, code, stdin,
                        timeoutMs != null ? timeoutMs.intValue() : 0,
                        memoryLimitKb != null ? memoryLimitKb.intValue() : 0);
                return CodingRunResultVO.builder()
                        .language(language)
                        .status(resolveJudgeStatus(jr))
                        .accepted(jr.accepted)
                        .stdout(jr.stdout)
                        .stderr(jr.stderr)
                        .compileOutput(jr.compileOutput)
                        .statusDescription(jr.statusDescription)
                        .exitCode(jr.exitCode)
                        .time(jr.time)
                        .memory(jr.memory)
                        .build();
            };
        }
        return new LocalCodeExecutor(defaultTimeoutMs);
    }

    private String resolveJudgeStatus(Judge0Client.JudgeResult result) {
        if (result.accepted || result.statusId == 3) {
            return "accepted";
        }
        return switch (result.statusId) {
            case 4 -> "wrong_answer";
            case 5 -> "time_limit_exceeded";
            case 6 -> "compilation_error";
            case 7, 8, 9, 10, 11, 12 -> "runtime_error";
            case 13 -> "internal_error";
            case 14 -> "exec_format_error";
            default -> result.statusId < 0 ? "sandbox_error" : "runtime_error";
        };
    }
}
