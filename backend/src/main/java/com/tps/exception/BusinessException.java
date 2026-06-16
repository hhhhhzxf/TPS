package com.tps.exception;

/**
 * 文件说明：异常类型定义，负责表达业务侧可识别的错误语义。
 */

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final HttpStatus status;

    public BusinessException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message, HttpStatus.CONFLICT);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(429, message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
