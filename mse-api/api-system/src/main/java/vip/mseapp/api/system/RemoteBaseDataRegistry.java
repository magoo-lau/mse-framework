package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.BaseScriptDescriptor;

import java.util.List;

/**
 * 基座库脚本注册契约（mse-system 实现，业务积木消费）。
 *
 * <p>业务积木自带 {@code db/<方言>/V*.sql} 中头指令 {@code -- mse-target: base} 的脚本（内容幂等），
 * 由 starter-bridge 的推送器在应用就绪后自动推送并周期重推，无需任何手工 SQL 登记。
 * 接收端（mse-system）按自身数据库方言筛取脚本，经 {@code mse_<biz>_schema_history} 分册账本
 * 只应用 pending：已应用不重跑（checksum 变化报 CONFLICT 提示）、失败记账后等下次推送重试。
 * 脚本自身幂等（NOT EXISTS / IF NOT EXISTS / 条件 UPDATE），重复推送与多实例并发推送无害；
 * 管理员对菜单/权限/子应用的启停与改名不被覆盖（NOT EXISTS 见行即跳）。
 */
public interface RemoteBaseDataRegistry {

    /**
     * 批量注册基座库脚本。
     *
     * @param scripts 脚本清单（全方言一并推送，接收端按自身方言筛取；无效条目跳过并告警，不拖垮整批）
     * @return 处理摘要（当前版本 → 应用/失败计数；管理端与日志可见）
     */
    String register(List<BaseScriptDescriptor> scripts);
}
