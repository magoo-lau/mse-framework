package vip.mseapp.core.result;

/**
 * 错误码契约：模块自定义错误码实现本接口即可被统一异常处理识别。
 */
public interface ErrorCode {

    /** 业务错误码（全局唯一） */
    int getCode();

    /** 默认提示文案 */
    String getMessage();
}
