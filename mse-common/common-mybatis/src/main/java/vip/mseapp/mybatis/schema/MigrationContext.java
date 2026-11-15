package vip.mseapp.mybatis.schema;

import java.util.Map;

/**
 * Java 迁移执行上下文：方言、租户、目标与占位符（已含内置 {@code tenant_id} 与配置的自定义项）。
 *
 * @param dbType       方言（mysql/dm/kingbase）
 * @param tenantId     租户编号（每租户通道=实际租户；结构通道=0）
 * @param target       目标库通道
 * @param placeholders 生效占位符（只读）
 */
public record MigrationContext(String dbType, long tenantId, MigrationTarget target,
                               Map<String, String> placeholders) {
}
