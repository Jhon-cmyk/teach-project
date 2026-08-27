package com.ruyi.teach.exception;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.common.TraceContext;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "请求参数校验失败";

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse<Map<String, String>> validationExceptionHandler(Exception e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (e instanceof MethodArgumentNotValidException validationException) {
            collectFieldErrors(validationException.getBindingResult().getFieldErrors(), fieldErrors);
        } else if (e instanceof BindException bindException) {
            collectFieldErrors(bindException.getBindingResult().getFieldErrors(), fieldErrors);
        }
        log.warn("Request validation failed, trace_id={}, fields={}",
                traceId(), fieldErrors.keySet());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, fieldErrors, VALIDATION_MESSAGE);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public BaseResponse<Map<String, String>> methodValidationExceptionHandler(
            HandlerMethodValidationException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();
            if (field == null || field.isBlank()) {
                field = "request";
            }
            String finalField = field;
            result.getResolvableErrors().forEach(error ->
                    fieldErrors.putIfAbsent(
                            finalField,
                            error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage()
                    )
            );
        }
        log.warn("Method parameter validation failed, trace_id={}, fields={}",
                traceId(), fieldErrors.keySet());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, fieldErrors, VALIDATION_MESSAGE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Map<String, String>> constraintViolationExceptionHandler(
            ConstraintViolationException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;
            fieldErrors.putIfAbsent(field, violation.getMessage());
        }
        log.warn("Request constraint validation failed, trace_id={}, fields={}",
                traceId(), fieldErrors.keySet());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, fieldErrors, VALIDATION_MESSAGE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Map<String, String>> messageNotReadableExceptionHandler() {
        log.warn("Request body is missing or malformed, trace_id={}", traceId());
        return ResultUtils.error(
                ErrorCode.PARAMS_ERROR,
                Map.of("request", "请求体缺失或 JSON 格式错误"),
                VALIDATION_MESSAGE
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public BaseResponse<Map<String, String>> typeMismatchExceptionHandler(
            MethodArgumentTypeMismatchException e) {
        String field = e.getName() == null ? "request" : e.getName();
        log.warn("Request parameter type mismatch, trace_id={}, field={}", traceId(), field);
        return ResultUtils.error(
                ErrorCode.PARAMS_ERROR,
                Map.of(field, "参数类型错误"),
                VALIDATION_MESSAGE
        );
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public BaseResponse<Map<String, String>> requestBindingExceptionHandler() {
        log.warn("Required request value is missing, trace_id={}", traceId());
        return ResultUtils.error(
                ErrorCode.PARAMS_ERROR,
                Map.of("request", "缺少必需的请求参数"),
                VALIDATION_MESSAGE
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<?> methodNotAllowedExceptionHandler(
            HttpRequestMethodNotSupportedException e) {
        log.warn("Request method is not supported, trace_id={}, method={}",
                traceId(), e.getMethod());
        return ResultUtils.error(ErrorCode.METHOD_NOT_ALLOWED_ERROR);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public BaseResponse<?> mediaTypeNotSupportedExceptionHandler() {
        log.warn("Request media type is not supported, trace_id={}", traceId());
        return ResultUtils.error(ErrorCode.MEDIA_TYPE_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<?> maxUploadSizeExceptionHandler() {
        log.warn("Uploaded file exceeds configured limit, trace_id={}", traceId());
        return ResultUtils.error(ErrorCode.FILE_TOO_LARGE_ERROR);
    }

    @ExceptionHandler(MultipartException.class)
    public BaseResponse<?> multipartExceptionHandler() {
        log.warn("Multipart request is invalid, trace_id={}", traceId());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "上传请求格式错误");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public BaseResponse<?> noResourceFoundExceptionHandler() {
        log.warn("Requested endpoint or resource was not found, trace_id={}", traceId());
        return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public BaseResponse<?> externalServiceExceptionHandler(ExternalServiceException e) {
        String causeType = e.getCause() == null
                ? "-"
                : e.getCause().getClass().getSimpleName();
        log.error("External service failed, trace_id={}, service={}, cause={}",
                traceId(), e.getServiceName(), causeType);
        log.debug("External service failure details, trace_id={}", traceId(), e);
        return ResultUtils.error(ErrorCode.EXTERNAL_SERVICE_ERROR, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public BaseResponse<?> dataConflictExceptionHandler(DataIntegrityViolationException e) {
        log.warn("Data integrity conflict, trace_id={}, exception={}",
                traceId(), e.getClass().getSimpleName());
        return ResultUtils.error(
                ErrorCode.DATA_CONFLICT_ERROR,
                "数据已存在或与现有记录冲突"
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse<?> dataAccessExceptionHandler(DataAccessException e) {
        log.error("Database access failed, trace_id={}", traceId(), e);
        return ResultUtils.error(ErrorCode.DATA_ACCESS_ERROR);
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        if (isUnexpectedServerError(e.getCode())) {
            log.error("Business operation failed unexpectedly, trace_id={}, code={}",
                    traceId(), e.getCode(), e);
        } else if (e.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            log.info("Authentication required, trace_id={}", traceId());
        } else {
            log.warn("Business request rejected, trace_id={}, code={}",
                    traceId(), e.getCode());
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> unknownExceptionHandler(Exception e) {
        log.error("Unhandled server exception, trace_id={}", traceId(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后重试");
    }

    private void collectFieldErrors(Iterable<FieldError> errors,
                                    Map<String, String> fieldErrors) {
        for (FieldError error : errors) {
            String field = "passwordConfirmed".equals(error.getField())
                    ? "checkPassword"
                    : error.getField();
            fieldErrors.putIfAbsent(
                    field,
                    error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage()
            );
        }
    }

    private boolean isUnexpectedServerError(int code) {
        return code == ErrorCode.SYSTEM_ERROR.getCode()
                || code == ErrorCode.DATA_ACCESS_ERROR.getCode();
    }

    private String traceId() {
        String traceId = TraceContext.currentTraceId();
        return traceId == null ? "-" : traceId;
    }
}
