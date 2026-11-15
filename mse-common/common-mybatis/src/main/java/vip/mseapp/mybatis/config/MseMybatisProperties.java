package vip.mseapp.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 公共配置（mse.mybatis.*）。
 */
@ConfigurationProperties(prefix = "mse.mybatis")
public class MseMybatisProperties {

    /**
     * 分页插件数据库方言：auto（默认，按当前数据源 JDBC URL 自动识别）或
     * MyBatis-Plus DbType 枚举名（大小写不敏感，如 mysql / dm / kingbase_es）。
     *
     * <p>auto 模式下逐语句从实际连接 URL 识别（jdbc:dm:→DM、jdbc:kingbase8:→KINGBASE_ES、
     * jdbc:mysql:→MYSQL），配合 dynamic-datasource 多数据源/混合租户部署无需逐库声明；
     * URL 被代理改写等特殊场景可显式指定。
     */
    private String dbType = "auto";

    /** 慢 SQL 采集（mse.mybatis.slow-sql.*）：默认关闭，显式 enabled=true 才装配拦截器 */
    private final SlowSql slowSql = new SlowSql();

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public SlowSql getSlowSql() {
        return slowSql;
    }

    /** 慢 SQL 采集参数：关闭时拦截器不注册（零开销）；开启后仅超阈值语句进入内存环形缓存 */
    public static class SlowSql {

        /** 是否启用慢 SQL 采集（默认 false） */
        private boolean enabled = false;

        /** 慢 SQL 阈值（毫秒），执行耗时达到即记录 */
        private long thresholdMs = 3000;

        /** 环形缓存条数（最近 N 条，超出淘汰最旧） */
        private int capacity = 32;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getThresholdMs() {
            return thresholdMs;
        }

        public void setThresholdMs(long thresholdMs) {
            this.thresholdMs = thresholdMs;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
}
