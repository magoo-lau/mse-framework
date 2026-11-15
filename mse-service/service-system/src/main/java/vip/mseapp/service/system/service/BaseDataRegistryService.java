package vip.mseapp.service.system.service;

import vip.mseapp.api.system.dto.BaseScriptDescriptor;

import java.util.List;

/**
 * 基座库脚本注册服务（db/base 通道接收端）：业务积木经 starter-bridge 推送的原 base.sql
 * 版本化脚本，按本服务数据库方言筛取后经 {@code mse_schema_history} 账本应用到基座库。
 */
public interface BaseDataRegistryService {

    /**
     * 应用推送的基座库脚本（幂等：已应用不重跑、checksum 变化报 CONFLICT、失败记账待重试）。
     *
     * @param scripts 全方言脚本清单（接收端按自身方言筛取）
     * @return 处理摘要（日志与推送端可见）
     */
    String register(List<BaseScriptDescriptor> scripts);
}
