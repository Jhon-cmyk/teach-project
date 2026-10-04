package com.ruyi.teach.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link TraceContext#wrap(Runnable)} 对不受 Spring 管理的线程池的上下文传递。
 */
class TraceContextTest {

    private static final long WAIT_SECONDS = 5;

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() throws InterruptedException {
        TraceContext.clear();
        pool.shutdownNow();
        pool.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void wrapPropagatesTraceIdToPooledThread() throws InterruptedException {
        TraceContext.bind("trace-wrap-001");
        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        pool.execute(TraceContext.wrap(() -> {
            seen.set(TraceContext.currentTraceId());
            done.countDown();
        }));

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "任务未完成");
        assertEquals("trace-wrap-001", seen.get());
    }

    @Test
    void wrapRestoresTargetThreadValueAfterRun() {
        TraceContext.bind("trace-outer");
        Runnable wrapped = TraceContext.wrap(() -> TraceContext.bind("trace-inner"));

        wrapped.run();

        assertEquals("trace-outer", TraceContext.currentTraceId(), "执行完毕后应还原为原有取值");
    }

    @Test
    void wrapCallablePropagatesTraceIdAndKeepsReturnValue() throws Exception {
        TraceContext.bind("trace-callable-001");
        ExecutorService callablePool = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = callablePool.submit(TraceContext.wrapCallable(TraceContext::currentTraceId));
            assertEquals("trace-callable-001", future.get(WAIT_SECONDS, TimeUnit.SECONDS));
        } finally {
            callablePool.shutdownNow();
        }
    }

    @Test
    void wrapClearsWhenSubmittingThreadHasNoTrace() throws InterruptedException {
        // 先让 worker 沾上一个 trace
        CountDownLatch seed = new CountDownLatch(1);
        pool.execute(() -> {
            TraceContext.bind("trace-stale");
            seed.countDown();
        });
        assertTrue(seed.await(WAIT_SECONDS, TimeUnit.SECONDS), "预热任务未完成");

        TraceContext.clear();
        AtomicReference<String> seen = new AtomicReference<>("unset");
        CountDownLatch done = new CountDownLatch(1);
        pool.execute(TraceContext.wrap(() -> {
            seen.set(TraceContext.currentTraceId());
            done.countDown();
        }));

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "任务未完成");
        assertNull(seen.get(), "提交线程无 trace 时，worker 不应残留旧值");
    }
}
