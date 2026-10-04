package com.ruyi.teach.config;

import com.ruyi.teach.common.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 trace 上下文跨线程池的传递与还原。
 */
class TraceTaskDecoratorTest {

    private static final long WAIT_SECONDS = 5;

    private final TraceTaskDecorator decorator = new TraceTaskDecorator();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() throws InterruptedException {
        TraceContext.clear();
        pool.shutdownNow();
        pool.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void propagatesSubmittingThreadTraceId() throws InterruptedException {
        TraceContext.bind("trace-parent-001");
        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        pool.execute(decorator.decorate(() -> {
            seen.set(TraceContext.currentTraceId());
            done.countDown();
        }));

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "任务未在超时前完成");
        assertEquals("trace-parent-001", seen.get(), "工作线程应看到提交线程的 trace_id");
    }

    @Test
    void doesNotLeakIntoReusedWorker() throws InterruptedException {
        TraceContext.bind("trace-first-001");
        CountDownLatch first = new CountDownLatch(1);
        pool.execute(decorator.decorate(first::countDown));
        assertTrue(first.await(WAIT_SECONDS, TimeUnit.SECONDS), "第一个任务未完成");

        // 提交线程已无 trace，同一个 worker 复用来跑第二个任务
        TraceContext.clear();
        AtomicReference<String> second = new AtomicReference<>("unset");
        CountDownLatch done = new CountDownLatch(1);
        pool.execute(decorator.decorate(() -> {
            second.set(TraceContext.currentTraceId());
            done.countDown();
        }));

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "第二个任务未完成");
        assertNull(second.get(), "复用 worker 时不应读到上一个任务的 trace_id");
    }

    @Test
    void restoresWorkerOwnContextAfterTask() throws InterruptedException {
        AtomicReference<String> innerSaw = new AtomicReference<>();
        AtomicReference<String> workerAfter = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        pool.execute(() -> {
            // 模拟 worker 上已经存在外层任务的上下文
            TraceContext.bind("trace-worker-own");
            Runnable decorated = decorator.decorate(() -> {
                innerSaw.set(TraceContext.currentTraceId());
                TraceContext.bind("trace-inner");
            });
            decorated.run();
            workerAfter.set(TraceContext.currentTraceId());
            TraceContext.clear();
            done.countDown();
        });

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "外层任务未完成");
        assertEquals("trace-worker-own", innerSaw.get(), "子任务应看到提交线程的上下文");
        assertEquals("trace-worker-own", workerAfter.get(), "任务结束后必须还原 worker 原有上下文");
    }

    @Test
    void appliesWhenRegisteredOnSpringExecutor() throws InterruptedException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setTaskDecorator(new TraceTaskDecorator());
        executor.initialize();
        try {
            TraceContext.bind("trace-tpte-001");
            AtomicReference<String> seen = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);

            executor.execute(() -> {
                seen.set(TraceContext.currentTraceId());
                done.countDown();
            });

            assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS), "任务未完成");
            assertEquals("trace-tpte-001", seen.get());
        } finally {
            executor.shutdown();
        }
    }
}
