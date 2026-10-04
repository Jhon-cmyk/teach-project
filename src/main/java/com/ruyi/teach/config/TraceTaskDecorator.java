package com.ruyi.teach.config;

import com.ruyi.teach.common.TraceContext;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 把提交线程的日志上下文传递给线程池的工作线程。
 *
 * <p>MDC 底层是 {@link ThreadLocal}，值挂在 {@link Thread} 对象自己的 map 上；任务提交给线程池后，
 * 执行它的是被复用的 worker 线程，与提交线程不是同一个 {@code Thread}。因此子线程既读不到父线程的
 * trace_id（日志断流），也可能读到上一个任务残留的值（日志串号）。
 *
 * <p>{@code TaskDecorator} 的 {@code decorate} 在任务提交时、于提交线程上被调用，是捕获上下文的正确
 * 时机。{@link InheritableThreadLocal} 不适用于线程池：它在 {@code new Thread()} 时才继承一次，
 * 与"worker 懒创建 + 长期复用"的模型冲突，会长期串号并造成 ThreadLocal 泄漏。
 *
 * <p>作用范围：只覆盖被装饰的执行器。{@code CompletableFuture} 默认使用的
 * ForkJoinPool.commonPool、原生 {@code new Thread()}、以及第三方库内部的线程池都不经过这里，
 * 这些调用点需要显式传递（见 {@link TraceContext#wrap(Runnable)}）。
 */
public class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        // 在提交线程上捕获：此处仍在父线程的上下文中
        String traceId = TraceContext.currentTraceId();
        Map<String, String> snapshot = MDC.getCopyOfContextMap();

        return () -> {
            // 工作线程可能已被外层任务占用，先保存它原有的上下文
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (snapshot == null) {
                    MDC.clear();
                } else {
                    // 必须用快照：传引用的话父线程后续修改会串进已提交的任务
                    MDC.setContextMap(snapshot);
                }
                TraceContext.bind(traceId);
                task.run();
            } finally {
                // 还原而不是直接清空：任务内部可能再次提交子任务，无脑 clear 会破坏外层上下文；
                // 还原同时保证 worker 不残留本次上下文（防串号）也不滞留引用（防泄漏）。
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }
}
