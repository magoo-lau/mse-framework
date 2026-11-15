package vip.mseapp.mybatis.datasource;

import vip.mseapp.mybatis.schema.SchemaMigration;

import java.util.List;

/**
 * 定义目录变更批次来源 SPI（新租户库懒初始化追平用）。
 *
 * <p>平台管理端对目录（sys_menu/sys_permission/sys_micro_app）的运行期编辑落平台库
 * {@code sys_catalog_change} 批次表，经车队调谐扇出到存量租户库；<b>新租户库</b>在懒初始化
 * 应用脚本基线后，经本 SPI 拉取全量批次追平（账本按 script 键幂等，重复应用无害），收敛到
 * 平台当前目录状态——即「声明式清单基线 + controller 调谐记录」二元结构的后者。
 *
 * <p>实现形态（starter-bridge 提供）：消费 api-system 的 {@code RemoteCatalogRegistry} 契约
 * （单体 local 直调 / 微服务 dubbo 远调，与 TenantDsMetadataProvider 的双通道范式一致）。
 * 实现缺席（未引 starter-bridge）时懒初始化跳过追平，由车队扇出兜底收敛。
 */
public interface CatalogChangeSource {

    /**
     * 拉取 id 大于 sinceId 的目录变更批次（按 id 升序）。
     *
     * @param sinceId 已追平的最大批次 id（全新库追平传 0）
     * @param limit   单次拉取上限（实现按此截断；调用方循环翻页直至取空）
     * @return 变更批次清单（biz=catalog、version=zeroPad10(id)、script=catalog/change/&lt;id&gt;；
     *         平台无批次或批次表未建时返回空清单）
     */
    List<SchemaMigration> changesSince(long sinceId, int limit);
}
