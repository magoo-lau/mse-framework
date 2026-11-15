package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.mybatis.schema.TenantDataMigrator;

import java.util.List;

/**
 * 每租户数据自动扫捕（mse.tenant.schema.tenant-data.auto-upgrade=true 时装配）：
 * 启动后拉取平台侧全部租户，逐租户补跑 pending 每租户脚本（账本 (script, tenant) 去重、
 * 已应用零成本跳过）——微服务无 MQ 时事件到达面受限的兜底通道。
 *
 * <p>租户清单经 {@link RemoteTenantService}（单体=本地直调；微服务=dubbo 消费 mse-platform，
 * {@code @RpcReference} 由桥接处理器注入）。
 */
public class TenantDataUpgradeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantDataUpgradeRunner.class);

    @RpcReference
    private RemoteTenantService remoteTenantService;

    private final TenantDataMigrator migrator;

    public TenantDataUpgradeRunner(TenantDataMigrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<TenantDTO> tenants = remoteTenantService.listTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.info("[mse-bridge] auto-upgrade：平台侧无租户，跳过每租户数据扫捕");
            return;
        }
        // 只扫共享库租户（dsKey 空白）：独立库租户的每租户种子由懒初始化直建通道在租户库内执行，
        // 在内置共享库跑 forTenant 脚本只会播出无人消费的孤儿行
        List<Long> tenantIds = tenants.stream()
                .filter(t -> t != null && t.getId() != null)
                .filter(t -> t.getDsKey() == null || t.getDsKey().isBlank())
                .map(TenantDTO::getId)
                .toList();
        if (tenantIds.isEmpty()) {
            log.info("[mse-bridge] auto-upgrade：无共享库租户（{} 个独立库租户由懒初始化覆盖），跳过",
                    tenants.size());
            return;
        }
        log.info("[mse-bridge] auto-upgrade：开始为 {} 个共享库租户补跑每租户脚本", tenantIds.size());
        List<Long> failed = migrator.migrateAll(tenantIds);
        if (failed.isEmpty()) {
            log.info("[mse-bridge] auto-upgrade：全部租户每租户脚本已就绪");
        } else {
            log.warn("[mse-bridge] auto-upgrade：部分租户失败（下次启动或管理端手动重试）: {}", failed);
        }
    }
}
