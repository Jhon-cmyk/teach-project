package com.ruyi.teach.common;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class TraceContext {

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String ATTRIBUTE_NAME = TraceContext.class.getName() + ".traceId";
    public static final String MDC_KEY = "trace_id";

    private static final Pattern SAFE_TRACE_ID =
            Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    private TraceContext() {
    }

    public static String resolveOrCreate(String candidate) {
        if (candidate != null && SAFE_TRACE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void bind(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            clear();
            return;
        }
        MDC.put(MDC_KEY, traceId);
    }

    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    /**
     * 包装一个任务，使其在目标线程上执行时带上调用方的 trace_id，执行完毕后还原目标线程原有取值。
     *
     * <p>用于不受 Spring 管理的线程池（{@code Executors.newXxxPool()}）：{@code TaskDecorator}
     * 只覆盖被装饰的执行器，这类池需要在此显式包装。
     */
    public static Runnable wrap(Runnable task) {
        String traceId = currentTraceId();
        return () -> {
            String previous = currentTraceId();
            try {
                bind(traceId);
                task.run();
            } finally {
                bind(previous);
            }
        };
    }

    /**
     * {@link #wrap(Runnable)} 的 Callable 版本：作用于提交给 {@code CompletionService} 等需要返回值的任务。
     *
     * <p>之所以另起名字而不是重载：lambda 同时匹配 {@code Runnable} 与 {@code Callable}，重载会产生歧义。
     */
    public static <T> Callable<T> wrapCallable(Callable<T> task) {
        String traceId = currentTraceId();
        return () -> {
            String previous = currentTraceId();
            try {
                bind(traceId);
                return task.call();
            } finally {
                bind(previous);
            }
        };
    }
}
