package vip.mseapp.rpc.bridge;

/**
 * 租户快照存储后端 SPI（数据面本地自治副本的载体；内存 L1 之外的 L2）。
 *
 * <p>载荷格式统一为 {@code payload + '\n' + signature + '\n' + version}（AES-GCM 密文 + HMAC-SHA256
 * 签名 + 平台发布版本），写入前已加密、读出后由 {@link TenantSnapshotStore} 验签解密。
 * 实现形态：在线部署=Redis（多实例共享一份快照）；离线部署=加密文件（无 Redis 依赖）。
 * 后端不可用（Redis 异常/文件缺失）一律降级返回 null，不阻断回源 RPC 通道。
 */
public interface SnapshotBackend {

    /** 读取租户快照载荷；无副本或后端不可用返回 null */
    String read(Long tenantId);

    /** 写入租户快照载荷（写穿）；后端不可用静默跳过（告警由实现负责） */
    void write(Long tenantId, String payload, String signature, long version);
}
