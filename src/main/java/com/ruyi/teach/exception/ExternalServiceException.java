package com.ruyi.teach.exception;

import lombok.Getter;

/**
 * 外部 API、对象存储、语音服务等依赖不可用时使用的异常。
 * 对客户端只暴露稳定文案，具体原因保留在 cause 和服务端日志中。
 */
@Getter
public class ExternalServiceException extends BusinessException {

    private final String serviceName;

    public ExternalServiceException(String serviceName, Throwable cause) {
        this(serviceName, ErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(), cause);
    }

    public ExternalServiceException(String serviceName, String publicMessage) {
        this(serviceName, publicMessage, null);
    }

    public ExternalServiceException(String serviceName, String publicMessage, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, publicMessage, cause);
        this.serviceName = serviceName;
    }
}
