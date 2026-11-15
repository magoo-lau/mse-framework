package vip.mseapp.web.aspect;

import vip.mseapp.core.annotation.TenantIgnore;
import vip.mseapp.core.context.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * {@link TenantIgnore} 切面：方法执行期间跳过租户隔离（平台端/定时任务专用）。
 */
@Aspect
public class TenantIgnoreAspect {

    @Around("@annotation(tenantIgnore) || @within(tenantIgnore)")
    public Object around(ProceedingJoinPoint joinPoint, TenantIgnore tenantIgnore) throws Throwable {
        boolean old = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            return joinPoint.proceed();
        } finally {
            TenantContext.setIgnoreTenant(old);
        }
    }
}
