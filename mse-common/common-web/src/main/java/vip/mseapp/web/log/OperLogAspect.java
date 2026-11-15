package vip.mseapp.web.log;

import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link OperLog} 切面：采集模块/方法/URL/参数/操作人/IP/耗时/结果，交给 {@link OperLogRecorder} 异步落库。
 *
 * <p>日志记录失败不影响业务（吞掉并仅打印 warn）；无 Recorder 实现时静默跳过。
 */
@Aspect
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    /** 参数/错误消息入库截断长度（与 sys_oper_log.oper_param / error_msg 列宽对齐） */
    private static final int MAX_TEXT_LENGTH = 2000;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 单线程异步落库：日志写慢不阻塞业务请求（进程退出时为 daemon 线程，不阻断停机） */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mse-oper-log");
        thread.setDaemon(true);
        return thread;
    });

    private final ObjectProvider<OperLogRecorder> recorderProvider;

    public OperLogAspect(ObjectProvider<OperLogRecorder> recorderProvider) {
        this.recorderProvider = recorderProvider;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        OperLogEvent event = new OperLogEvent();
        event.setModule(operLog.module());
        event.setOperType(operLog.type());
        event.setMethod(joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName());
        fillRequest(event);
        UserContext.UserInfo user = UserContext.get();
        if (user != null) {
            event.setOperUserId(user.userId());
            event.setOperUsername(user.username());
        }
        event.setTenantId(TenantContext.getTenantId());
        event.setOperParam(toJson(filterArgs(joinPoint.getArgs())));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            event.setStatus(0);
            return result;
        } catch (Throwable ex) {
            event.setStatus(1);
            event.setErrorMsg(truncate(ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage()));
            throw ex;
        } finally {
            event.setCostMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
            event.setOperTime(LocalDateTime.now());
            submit(event);
        }
    }

    private void submit(OperLogEvent event) {
        OperLogRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder == null) {
            log.debug("操作日志跳过（无 OperLogRecorder 实现）：{}", event.getMethod());
            return;
        }
        executor.execute(() -> {
            try {
                recorder.record(event);
            } catch (Exception ex) {
                log.warn("操作日志记录失败：{}", event.getMethod(), ex);
            }
        });
    }

    /** 从当前请求线程补全 URL/请求方式/IP；无请求上下文（MQ/任务）时保持空 */
    private void fillRequest(OperLogEvent event) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            event.setRequestMethod(request.getMethod());
            event.setOperUrl(truncateUrl(request.getRequestURI()));
            event.setOperIp(resolveIp(request));
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int idx = forwarded.indexOf(',');
            return idx > 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** 排除无法序列化/无审计意义的入参（Servlet 对象、上传文件二进制） */
    private Object[] filterArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest)
                        && !(arg instanceof HttpServletResponse)
                        && !(arg instanceof MultipartFile)
                        && !(arg instanceof MultipartFile[]))
                .toArray();
    }

    private String toJson(Object value) {
        try {
            return truncate(JSON.writeValueAsString(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String truncateUrl(String url) {
        return url != null && url.length() > 255 ? url.substring(0, 255) : url;
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
