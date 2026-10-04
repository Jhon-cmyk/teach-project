package com.ruyi.teach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class VideoTimelineAnalysisExecutorConfig {

    @Bean(name = "videoTimelineAnalysisExecutor")
    public Executor videoTimelineAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("video-timeline-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 把提交线程的 trace_id 带到工作线程，日志与响应头保持一致
        executor.setTaskDecorator(new TraceTaskDecorator());
        executor.initialize();
        return executor;
    }
}
