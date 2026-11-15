package vip.mseapp.mybatis.slowsql;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;

import java.sql.Statement;
import java.util.Properties;

/**
 * 慢 SQL 拦截器：在 StatementHandler 层计时，超阈值语句交给 {@link SlowSqlCollector}。
 *
 * <p>未开启时不装配本类（mse.mybatis.slow-sql.enabled，默认 false）；开启后每条语句
 * 仅一次 nanoTime 计时 + 阈值比较，正常语句无额外动作。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query",
                args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private final SlowSqlCollector collector;

    public SlowSqlInterceptor(SlowSqlCollector collector) {
        this.collector = collector;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            if (collector.exceeds(elapsedMs)) {
                collector.record(mapperIdOf(invocation), sqlOf(invocation), elapsedMs);
            }
        }
    }

    /** RoutingStatementHandler 包一层 delegate，经 MetaObject 取真实 handler 上的语句元数据 */
    private String mapperIdOf(Invocation invocation) {
        try {
            MetaObject meta = SystemMetaObject.forObject(invocation.getTarget());
            Object ms = meta.getValue("delegate.mappedStatement");
            return ms instanceof MappedStatement mapped ? mapped.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String sqlOf(Invocation invocation) {
        try {
            MetaObject meta = SystemMetaObject.forObject(invocation.getTarget());
            Object boundSql = meta.getValue("delegate.boundSql");
            return boundSql instanceof BoundSql bs ? bs.getSql() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 配置统一走 mse.mybatis.slow-sql.*，无插件级 properties
    }
}
