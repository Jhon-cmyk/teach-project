package com.ruyi.teach.common;

import org.slf4j.MDC;

import java.util.UUID;
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
}
