package vip.mseapp.mybatis.schema;

import java.util.Map;

/**
 * 一次迁移的执行选项：目标通道 + 结构/每租户通道 + 基线策略。
 *
 * @param target          目标库通道（master/base/platform）
 * @param perTenant       false=结构通道（tenant_id=0 记账，DDL+平台数据，目标库级一次）；
 *                        true=每租户通道（tenant_id=实际租户记账，种子数据每租户一次）
 * @param tenantId        每租户通道的租户编号（结构通道忽略，按 0 记账）
 * @param placeholders    额外占位符（内置 tenant_id 自动注入）
 * @param autoBaseline    存量库自动基线（仅结构通道：账本空且库已有<b>该 biz 自身</b>业务表——按建表脚本解析的探针表判定，多积木共库不因他 biz 表而误判——时写 BASELINE 记账）
 * @param baselineVersion 手动指定基线版本（所有 biz 共用；懒初始化传主库版本；null=取各 biz 清单最大版本）
 * @param bizBaselines    按 biz 显式基线（懒初始化逐账本取主库版本；优先于 baselineVersion）
 */
public record MigrateOptions(MigrationTarget target, boolean perTenant, long tenantId,
                             Map<String, String> placeholders, boolean autoBaseline,
                             String baselineVersion, Map<String, String> bizBaselines) {

    /** 结构通道选项（tenant_id=0 记账） */
    public static MigrateOptions structure(MigrationTarget target, boolean autoBaseline) {
        return new MigrateOptions(target, false, 0, Map.of(), autoBaseline, null, Map.of());
    }

    /** 每租户通道选项（tenant_id=实际租户记账，占位符 ${tenant_id} 注入） */
    public static MigrateOptions forTenant(long tenantId, Map<String, String> placeholders) {
        return new MigrateOptions(MigrationTarget.MASTER, true, tenantId,
                placeholders == null ? Map.of() : placeholders, false, null, Map.of());
    }

    public MigrateOptions withBaseline(String baselineVersion) {
        return new MigrateOptions(target, perTenant, tenantId, placeholders, autoBaseline, baselineVersion, bizBaselines);
    }

    public MigrateOptions withBizBaselines(Map<String, String> bizBaselines) {
        return new MigrateOptions(target, perTenant, tenantId, placeholders, autoBaseline, baselineVersion,
                bizBaselines == null ? Map.of() : bizBaselines);
    }
}
