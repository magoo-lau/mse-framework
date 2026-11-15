package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.BaseScriptDescriptor;

import java.util.List;

/**
 * 定义目录变更批次契约（mse-system 实现，数据面懒初始化消费）。
 *
 * <p>平台管理端对目录（sys_menu/sys_permission/sys_micro_app）的运行期编辑落平台库
 * {@code sys_catalog_change} 批次表；新租户库懒初始化在脚本基线之上经本契约拉取全量批次追平，
 * 收敛到平台当前目录状态。批次以 {@link BaseScriptDescriptor} 为载体：biz=catalog、
 * version=zeroPad10(id)（字典序=时间序）、script=catalog/change/&lt;id&gt;（账本去重键）、
 * sql=幂等 ANSI 变更语句、checksum=sql 摘要。
 *
 * <p>批次表未建或平台无编辑批次时返回空清单；拉取失败抛异常由调用方降级（车队扇出兜底收敛）。
 */
public interface RemoteCatalogRegistry {

    /**
     * 拉取 id 大于 sinceId 的目录变更批次（按 id 升序）。
     *
     * @param sinceId 已追平的最大批次 id（全新库追平传 0）
     * @param limit   单次拉取上限（截断；调用方翻页直至取空）
     * @return 批次清单（空=无待追平变更）
     */
    List<BaseScriptDescriptor> listChanges(long sinceId, int limit);
}
