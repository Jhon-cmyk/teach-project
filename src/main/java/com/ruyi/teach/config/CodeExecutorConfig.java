package com.ruyi.teach.config;

import com.ruyi.teach.service.CodeExecutor;
import com.ruyi.teach.client.Judge0Client;
import com.ruyi.teach.service.LocalCodeExecutor;
import com.ruyi.teach.model.vo.CodingRunResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutorConfig.class);

    @Value("${judge0.default-timeout-ms:5000}")
    private long defaultTimeoutMs;

    /**
     * 缺省值取 judge0 而不是 local：配置缺失时应当退化为"沙箱执行失败（可见）"，
     * 而不是"无沙箱执行（静默）"。
     */
    @Value("${code-executor.mode:judge0}")
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
        log.warn("代码执行器使用 LocalCodeExecutor（无隔离）：提交的代码将以应用进程身份在本机执行，"
                + "且内存上限不会被强制。仅限本地开发使用；生产环境请设置 CODE_EXECUTOR_MODE=judge0。");
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
