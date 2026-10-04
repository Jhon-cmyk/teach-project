package com.ruyi.teach.config;

import com.ruyi.teach.common.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证默认 {@code @Async} 执行器的装配。
 *
 * <p>项目里存在多个具名执行器，若没有名为 {@code taskExecutor} 的 bean，
 * Spring 会让不带限定符的 {@code @Async} 回退到 {@code SimpleAsyncTaskExecutor}
 * ——每任务新建线程且没有上限。这里把该假设固化成测试。
 */
class AsyncExecutorConfigTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void defaultAsyncExecutorIsADecoratedThreadPool() throws InterruptedException {
        Executor executor = new AsyncExecutorConfig().taskExecutor();
        assertTrue(executor instanceof ThreadPoolTaskExecutor,
                "裸 @Async 必须落在受管线程池上，否则会回退到 SimpleAsyncTaskExecutor");
        ThreadPoolTaskExecutor pooled = (ThreadPoolTaskExecutor) executor;

        try {
            TraceContext.bind("trace-async-config-001");
            AtomicReference<String> seen = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);

            pooled.execute(() -> {
                seen.set(TraceContext.currentTraceId());
                done.countDown();
            });

            assertTrue(done.await(5, TimeUnit.SECONDS), "任务未在超时前完成");
            assertEquals("trace-async-config-001", seen.get(), "默认执行器必须带 trace 装饰器");
        } finally {
            pooled.shutdown();
        }
    }
}
