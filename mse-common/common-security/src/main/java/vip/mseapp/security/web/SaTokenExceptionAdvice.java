package vip.mseapp.security.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常 → 统一 Result（优先级高于 common-web 的兜底异常处理）。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SaTokenExceptionAdvice {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> notLogin(NotLoginException e) {
        // 401 细分提示：被顶/被踢/过期语义不同，前端 401 提示优先取本消息
        if (NotLoginException.KICK_OUT.equals(e.getType())) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "您的账号已被强制下线，请重新登录");
        }
        if (NotLoginException.BE_REPLACED.equals(e.getType())) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "您的账号已在其他设备登录，如非本人操作请及时修改密码");
        }
        if (NotLoginException.TOKEN_TIMEOUT.equals(e.getType())) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
        }
        return Result.fail(GlobalErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> notPermission(NotPermissionException e) {
        return Result.fail(GlobalErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> notRole(NotRoleException e) {
        return Result.fail(GlobalErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(SaTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> saToken(SaTokenException e) {
        return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), e.getMessage());
    }
}
