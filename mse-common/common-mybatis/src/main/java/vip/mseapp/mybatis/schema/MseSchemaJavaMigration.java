package vip.mseapp.mybatis.schema;

import java.sql.Connection;

/**
 * Java 代码迁移（Flyway JavaMigration 式）：SQL 表达不了的初始化（引擎建表、数据加工等）以
 * Spring Bean 实现，与 SQL 脚本同一账本、同一版本序编排（业务积木在自己的 autoconfigure 里
 * {@code @Bean} 声明，自动装配收集）。
 *
 * <p>仅支持 master / 每租户通道——base 通道跨服务推送无法携带代码，基座库初始化仍用 SQL 脚本。
 * version 与 SQL 脚本同一命名空间：如类对应 {@code V20260901_10_oa__flowable_schema} 时
 * biz=oa、version=20260901_10；账本不记 checksum，行为变更靠新版本类表达。
 */
public interface MseSchemaJavaMigration {

    /** 服务标识（账本路由：mse_&lt;biz&gt;_schema_history） */
    String biz();

    /** 版本号（yyyyMMdd_NN，同 biz 内自增、字典序即时间序） */
    String version();

    /** 描述（账本展示） */
    String description();

    /** 目标库（缺省 master） */
    default MigrationTarget target() {
        return MigrationTarget.MASTER;
    }

    /** 是否每租户执行一次（缺省否）；true 时按租户记账、上下文携带 tenantId */
    default boolean perTenant() {
        return false;
    }

    /** 执行体：拿到的连接即目标库连接；抛异常=失败入账本并阻断本轮迁移 */
    void migrate(Connection connection, MigrationContext context) throws Exception;
}
