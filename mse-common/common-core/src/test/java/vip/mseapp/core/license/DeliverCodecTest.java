package vip.mseapp.core.license;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 部署包签名往返：平台私钥签名 → 租户公钥验签；篡改/错钥必须失败。
 */
class DeliverCodecTest {

    private static String privateKeyBase64;
    private static String publicKeyBase64;
    private static String otherPublicKeyBase64;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        KeyPair other = generator.generateKeyPair();
        otherPublicKeyBase64 = Base64.getEncoder().encodeToString(other.getPublic().getEncoded());
    }

    @Test
    void signAndVerifyRoundTrip() {
        byte[] manifest = "{\"formatVersion\":1,\"tenant\":\"demo\"}".getBytes(StandardCharsets.UTF_8);
        String signature = DeliverCodec.sign(manifest, privateKeyBase64);
        DeliverCodec.verify(manifest, signature, publicKeyBase64);
    }

    @Test
    void tamperedPayloadFails() {
        byte[] manifest = "manifest-bytes".getBytes(StandardCharsets.UTF_8);
        String signature = DeliverCodec.sign(manifest, privateKeyBase64);
        byte[] tampered = "manifest-bytes!".getBytes(StandardCharsets.UTF_8);
        assertThrows(DeliverCodec.DeliverSignatureException.class,
                () -> DeliverCodec.verify(tampered, signature, publicKeyBase64));
    }

    @Test
    void wrongPublicKeyFails() {
        byte[] manifest = "manifest-bytes".getBytes(StandardCharsets.UTF_8);
        String signature = DeliverCodec.sign(manifest, privateKeyBase64);
        assertThrows(DeliverCodec.DeliverSignatureException.class,
                () -> DeliverCodec.verify(manifest, signature, otherPublicKeyBase64));
    }

    @Test
    void licenseKeyPairWorksForDeliver() {
        // 与 license 同一密钥体系：LicenseCodec 密钥解析出来的键可直接签部署包
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        KeyPair pair = generator.generateKeyPair();
        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        LicensePayload payload = new LicensePayload("LIC-1", "tenant_a", "租户A", "标准版",
                java.util.List.of(1L, 2L), java.util.Map.of(), 1L, 2L);
        String token = LicenseCodec.issue(payload, priv);
        LicenseCodec.verify(token, pub);
        byte[] manifest = "shared-key-manifest".getBytes(StandardCharsets.UTF_8);
        DeliverCodec.verify(manifest, DeliverCodec.sign(manifest, priv), pub);
    }

    @Test
    void publicKeyDerivedFromPrivateKey() {
        // 公钥内置：平台只配私钥即可派生公钥内嵌交付物，派生公钥与真实公钥等价（可验签、错钥拒绝）
        byte[] payload = "derive-manifest".getBytes(StandardCharsets.UTF_8);
        String derived = LicenseCodec.publicKeyBase64Of(privateKeyBase64);
        DeliverCodec.verify(payload, DeliverCodec.sign(payload, privateKeyBase64), derived);
        assertThrows(DeliverCodec.DeliverSignatureException.class,
                () -> DeliverCodec.verify(payload, DeliverCodec.sign(payload, privateKeyBase64), otherPublicKeyBase64));
    }
}
