package vip.mseapp.service.system.rpc;

import org.springframework.util.StringUtils;
import vip.mseapp.api.system.RemoteAuthService;
import vip.mseapp.api.system.RemoteAuthService.EmailChallenge;
import vip.mseapp.api.system.RemoteAuthService.TotpEnrollment;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.service.system.service.TwoFactorService;

/**
 * 登录双因子认证契约实现（认证服务 service-auth 消费）。
 *
 * <p>所有方法显式携带租户参数，TwoFactorService 内部 runAs，保证登录链路（无租户上下文）语义正确；
 * TOTP 密钥/邮箱验证码本体不出本服务，消费方只拿布尔结果与绑定挑战等非敏感数据。
 */
@RpcService(RemoteAuthService.class)
public class RemoteAuthServiceImpl implements RemoteAuthService {

    private final TwoFactorService twoFactorService;

    public RemoteAuthServiceImpl(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @Override
    public boolean verifyTotp(Long tenantId, Long userId, String code) {
        return twoFactorService.verifyTotp(tenantId, userId, code);
    }

    @Override
    public EmailChallenge sendLoginEmailCode(Long tenantId, Long userId) {
        String maskedEmail = twoFactorService.sendLoginEmailCode(tenantId, userId);
        return new EmailChallenge(true, StringUtils.hasText(maskedEmail) ? maskedEmail : null);
    }

    @Override
    public boolean verifyEmailCode(Long tenantId, Long userId, String code) {
        return twoFactorService.verifyEmailCode(tenantId, userId, code);
    }

    @Override
    public TotpEnrollment createPendingTotp(Long tenantId, Long userId) {
        return twoFactorService.createPendingTotp(tenantId, userId);
    }

    @Override
    public boolean confirmPendingTotp(Long tenantId, Long userId, String code) {
        return twoFactorService.confirmPendingTotp(tenantId, userId, code);
    }

    @Override
    public void resetTenantAdminTotp(Long tenantId, String username) {
        twoFactorService.resetTenantAdminTotp(tenantId, username);
    }

    @Override
    public boolean isEmailChannelAvailable(Long tenantId, Long userId) {
        return twoFactorService.isEmailChannelAvailable(tenantId, userId);
    }
}
