package vip.mseapp.web.handler;

import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常带错误码返回，未知异常统一 500 并记录日志。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public Result<Void> serviceException(ServiceException e) {
        log.warn("[mse] 业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> methodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> constraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /** 请求体不可读（JSON 格式/编码错误）：客户端问题，归 400 而非落入 500 系统异常 */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> messageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("[mse] 请求体解析失败: {}", e.getMessage());
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请求体格式错误（JSON 解析失败）");
    }

    /** 上传超限（multipart 上限在业务校验之前触发）：客户端问题，归 400 而非落入 500 系统异常 */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> maxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("[mse] 上传文件超限: {}", e.getMessage());
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "上传文件过大，超出服务端允许的大小限制");
    }

    /** 缺少 multipart 分部（如漏传 file 字段）：客户端问题，归 400 而非落入 500 系统异常 */
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> missingServletRequestPart(org.springframework.web.multipart.support.MissingServletRequestPartException e) {
        log.warn("[mse] 缺少 multipart 分部: {}", e.getMessage());
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "缺少上传文件字段：" + e.getRequestPartName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> methodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(GlobalErrorCode.METHOD_NOT_ALLOWED);
    }

    /** 唯一键冲突（判重查询因逻辑删除看不见软删行、或并发窗口内重复）：归 400 业务错误而非 500 系统异常 */
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> duplicateKey(org.springframework.dao.DuplicateKeyException e) {
        log.warn("[mse] 唯一键冲突: {}", e.getMessage());
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "保存失败：编码/键名已存在（可能被已删除记录占用，请换一个）");
    }

    /** 请求参数缺失/类型不匹配：客户端问题，归 400 而非落入 500 系统异常 */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> badRequestParam(Exception e) {
        log.warn("[mse] 请求参数错误: {}", e.getMessage());
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请求参数缺失或格式不正确");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> noResourceFound(NoResourceFoundException e) {
        return Result.fail(GlobalErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> exception(Exception e) {
        log.error("[mse] 系统异常", e);
        return Result.fail(GlobalErrorCode.INTERNAL_ERROR);
    }
}
