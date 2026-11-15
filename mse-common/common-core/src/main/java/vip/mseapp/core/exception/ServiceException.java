package vip.mseapp.core.exception;

import vip.mseapp.core.result.ErrorCode;

import java.io.Serializable;

/**
 * 业务异常：携带错误码，由 common-web 的全局异常处理器统一转换为 Result。
 */
public class ServiceException extends RuntimeException implements Serializable {

    private final int code;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}
