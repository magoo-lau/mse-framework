package vip.mseapp.api.system;

import java.util.List;

/**
 * 鉴权数据源契约（service-system 实现）：权限码 / 角色 code 集合解析。
 *
 * <p>供未装配 service-system 的业务微服务实现 Sa-Token {@code StpInterface}
 * （见 starter-bridge）：登录态由网关校验，细粒度权限码在业务服务本地注解校验，
 * 数据经本契约向 mse-system 拉取。超管/租户套餐裁剪语义与基座本地实现一致。
 *
 * @param loginType 登录端（platform / tenant，与 Sa-Token loginType 一致）
 * @param loginId   Sa-Token loginId：平台端为用户编号；租户端为 "{tenantId}-{userId}"
 */
public interface RemoteAccessService {

    /** 用户权限码集合（平台超管 = 通配 *；租户管理员 = 套餐域内全量码） */
    List<String> getPermissionList(String loginType, String loginId);

    /** 用户角色编码集合 */
    List<String> getRoleList(String loginType, String loginId);
}
