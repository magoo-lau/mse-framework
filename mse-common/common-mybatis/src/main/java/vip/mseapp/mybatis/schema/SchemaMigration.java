package vip.mseapp.mybatis.schema;

/**
 * 单条结构迁移单元（SQL 脚本 / Java 迁移 / 基线 / 界面临时 SQL 统一建模）。
 *
 * @param biz         服务标识（账本路由：mse_&lt;biz&gt;_schema_history；ADHOC 为 adhoc）
 * @param version     版本号（{@code V20260922_01_oa__init.sql} → {@code 20260922_01}，同 biz 内字典序即时间序；ADHOC 为执行时刻串）
 * @param description 描述（文件名 {@code __} 后段 / 临时 SQL 摘要）
 * @param type        {@link #TYPE_BASELINE} / {@link #TYPE_CLASSPATH} / {@link #TYPE_JAVA} / {@link #TYPE_ADHOC}
 * @param target      目标库通道（master/base/platform）
 * @param perTenant   true=每租户通道（每租户执行一次、按租户记账，${tenant_id} 注入）
 * @param script      来源（classpath 路径 / Java 类名 / adhoc 标记），账本去重键
 * @param sql         脚本内容（模板原文；Java 迁移为 null）
 * @param checksum    模板原文规范化 SHA-256（检测已应用脚本被改动；Java 迁移为 null）
 * @param baseScope   base 通道落库域（仅 target=base 有意义，见 {@link BaseScope}；非 base 通道忽略）
 */
public record SchemaMigration(String biz, String version, String description, String type,
                              MigrationTarget target, boolean perTenant,
                              String script, String sql, String checksum, BaseScope baseScope) {

    public static final String TYPE_BASELINE = "BASELINE";
    public static final String TYPE_CLASSPATH = "CLASSPATH";
    public static final String TYPE_JAVA = "JAVA";
    public static final String TYPE_ADHOC = "ADHOC";

    /** 兼容构造（缺省租户域：历史调用点与 Java 迁移无需感知 base 域指令） */
    public SchemaMigration(String biz, String version, String description, String type,
                           MigrationTarget target, boolean perTenant,
                           String script, String sql, String checksum) {
        this(biz, version, description, type, target, perTenant, script, sql, checksum, BaseScope.TENANT);
    }
}
