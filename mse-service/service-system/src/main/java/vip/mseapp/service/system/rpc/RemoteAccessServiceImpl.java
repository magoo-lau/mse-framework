package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteAccessService;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.service.system.security.AccessCodeCalculator;
import vip.mseapp.service.system.security.AccessService;
import vip.mseapp.service.system.service.SysUserService;

import java.util.List;

/**
 * 鉴权数据源契约实现（计算语义见 {@link AccessCodeCalculator}）。
 *
 * <p>消费方（starter-bridge 的 RPC 版 StpInterface）自行持有 Redis 版本缓存，
 * 本实现每次现算——缓存未命中才走到这里，频率与基座本地路径的回源一致。
 */
@RpcService(RemoteAccessService.class)
public class RemoteAccessServiceImpl implements RemoteAccessService {

    private final SysUserService userService;
    private final AccessService accessService;

    public RemoteAccessServiceImpl(SysUserService userService, AccessService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    @Override
    public List<String> getPermissionList(String loginType, String loginId) {
        return List.copyOf(AccessCodeCalculator.permissions(userService, accessService, loginType, loginId));
    }

    @Override
    public List<String> getRoleList(String loginType, String loginId) {
        return List.copyOf(AccessCodeCalculator.roles(accessService, loginType, loginId));
    }
}
