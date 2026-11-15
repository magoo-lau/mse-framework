package vip.mseapp.core.license;

import java.security.Signature;
import java.util.Base64;

/**
 * 离线交付包签名（RS256 对任意字节）：与授权凭证共用平台同一把 RSA 密钥对
 * （{@code mse.license.private-key} / {@code mse.license.public-key}，PKCS#8/X.509
 * Base64 单行约定），平台侧对 manifest 规范化字节签名，租户侧用 license 公钥验签——
 * 一把公钥两用（验 license + 验部署包），租户侧零新增配置。
 */
public final class DeliverCodec {

    private DeliverCodec() {
    }

    /** 签名：返回 Base64 签名串；私钥非法/签名失败抛 IllegalStateException */
    public static String sign(byte[] payload, String privateKeyBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(LicenseCodec.privateKey(privateKeyBase64));
            signature.update(payload);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("部署包签名失败: " + e.getMessage(), e);
        }
    }

    /** 验签：签名不符/格式非法抛 {@link DeliverSignatureException}（导入侧按验签失败处置，不继续导入） */
    public static void verify(byte[] payload, String signatureBase64, String publicKeyBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            throw new DeliverSignatureException("部署包签名为空");
        }
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(LicenseCodec.publicKey(publicKeyBase64));
            verifier.update(payload);
            if (!verifier.verify(Base64.getDecoder().decode(signatureBase64.trim()))) {
                throw new DeliverSignatureException("部署包签名校验失败（非平台签发或已篡改）");
            }
        } catch (DeliverSignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new DeliverSignatureException("部署包验签失败: " + e.getMessage(), e);
        }
    }

    /** 部署包签名无效（验签失败/格式非法） */
    public static class DeliverSignatureException extends RuntimeException {

        public DeliverSignatureException(String message) {
            super(message);
        }

        public DeliverSignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
