package vip.mseapp.service.platform.service;

import vip.mseapp.api.tenant.dto.EntitlementDTO;

import java.util.List;
import java.util.Map;

/**
 * 租户权益计算（控制面权威口径）：权益 = 套餐 menuIds + 权益项链
 * default → 套餐 → 附加项(×qty) → 租户覆盖（后者优先；覆盖为绝对值）。
 */
public interface EntitlementService {

    /** 计算租户权益快照（永不为 null；未配套餐 menuIds=null 宽松语义） */
    EntitlementDTO compute(Long tenantId);

    /** 全局定义变更（权益项/套餐项/附加项定义）后调用：INCR 全局版本 + FEATURE_CHANGED 事件 */
    void evictGlobal();

    /** 租户级变更（订购附加项/覆盖/换绑套餐）后调用：INCR 租户版本 + FEATURE_CHANGED 事件 */
    void evictTenant(Long tenantId);

    /** 原始缓存读取（resync 对账用，不加载） */
    EntitlementDTO peekCached(Long tenantId);

    /** 全部租户编号（resync 对账用） */
    List<Long> allTenantIds();

    /** 缓存版本读取（resync 对账用） */
    Map<String, Long> cacheVersions(Long tenantId);
}
