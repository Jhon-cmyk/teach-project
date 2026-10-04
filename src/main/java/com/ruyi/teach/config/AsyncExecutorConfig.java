package com.ruyi.teach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 未指定执行器的 {@code @Async} 所使用的默认线程池。
 *
 * <p>项目里已经有两个具名执行器（{@code agentIndexExecutor}、{@code videoTimelineAnalysisExecutor}），
 * 因此 Spring Boot 的自动配置不再创建 {@code taskExecutor}；此时不带限定符的 {@code @Async}
 * 会回退到 {@code SimpleAsyncTaskExecutor}——它每个任务新建一个线程且没有上限，既拿不到 trace_id，
 * 也没有并发保护。这里补一个具名 {@code taskExecutor}，并挂上 {@link TraceTaskDecorator}。
 */
@Configuration
public class AsyncExecutorConfig {

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        // 队列满时交由调用线程执行，形成背压，避免任务被静默丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new TraceTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
