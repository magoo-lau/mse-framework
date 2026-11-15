package vip.mseapp.core.schedule;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** cron 解析（Spring {@link CronExpression}，六位表达式语义与 @Scheduled 一致） */
public class SpringCronExpressionResolver implements CronExpressionResolver {

    @Override
    public Instant nextExecution(String cron, Instant after) {
        ZonedDateTime next = CronExpression.parse(cron).next(after.atZone(ZoneId.systemDefault()));
        return next == null ? null : next.toInstant();
    }
}
