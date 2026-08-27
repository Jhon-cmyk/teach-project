package com.ruyi.teach.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    METHOD_NOT_ALLOWED_ERROR(40500, "请求方法不支持"),
    DATA_CONFLICT_ERROR(40900, "数据冲突"),
    FILE_TOO_LARGE_ERROR(41300, "上传文件过大"),
    MEDIA_TYPE_ERROR(41500, "请求媒体类型不支持"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    DATA_ACCESS_ERROR(50002, "数据访问失败"),
    EXTERNAL_SERVICE_ERROR(50200, "外部服务暂时不可用");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
