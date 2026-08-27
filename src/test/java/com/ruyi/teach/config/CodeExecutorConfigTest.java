package com.ruyi.teach.config;

import com.ruyi.teach.model.vo.CodingRunResultVO;
import com.ruyi.teach.service.CodeExecutor;
import com.ruyi.teach.client.Judge0Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeExecutorConfigTest {

    private final Judge0Client judge0Client = mock(Judge0Client.class);
    private final CodeExecutorConfig config = new CodeExecutorConfig();

    @BeforeEach
    void useJudge0Mode() {
        ReflectionTestUtils.setField(config, "mode", "judge0");
        ReflectionTestUtils.setField(config, "defaultTimeoutMs", 5000L);
    }

    @Test
    void judge0TimeoutIsReportedAsTimeLimitExceeded() {
        Judge0Client.JudgeResult timeout = new Judge0Client.JudgeResult();
        timeout.statusId = 5;
        timeout.statusDescription = "Time Limit Exceeded";
        when(judge0Client.submitAndWait(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(timeout);

        CodeExecutor executor = config.codeExecutor(judge0Client);
        CodingRunResultVO result = executor.execute("java", "class Main {}", "", 1000L, 65536L);

        assertThat(result.getAccepted()).isFalse();
        assertThat(result.getStatus()).isEqualTo("time_limit_exceeded");
        assertThat(result.getStatusDescription()).isEqualTo("Time Limit Exceeded");
    }

    @Test
    void judge0ServiceFailureIsReportedAsSandboxError() {
        when(judge0Client.submitAndWait(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Judge0Client.JudgeResult.error("Judge0 unavailable"));

        CodeExecutor executor = config.codeExecutor(judge0Client);
        CodingRunResultVO result = executor.execute("python", "print(1)", "", 1000L, 65536L);

        assertThat(result.getAccepted()).isFalse();
        assertThat(result.getStatus()).isEqualTo("sandbox_error");
        assertThat(result.getStatusDescription()).isEqualTo("Judge0 unavailable");
    }
}
