package vip.mseapp.core.result;

import java.io.Serializable;

/**
 * 统一 HTTP / RPC 返回体。
 *
 * @param <T> 数据类型
 */
public record Result<T>(int code, String message, T data) implements Serializable {

    public static <T> Result<T> ok() {
        return new Result<>(GlobalErrorCode.SUCCESS.getCode(), "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(GlobalErrorCode.SUCCESS.getCode(), "success", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return code == GlobalErrorCode.SUCCESS.getCode();
    }
}
