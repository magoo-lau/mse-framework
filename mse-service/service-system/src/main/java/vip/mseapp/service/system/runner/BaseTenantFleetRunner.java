package vip.mseapp.service.system.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import vip.mseapp.mybatis.datasource.TenantFleetBaseReplayer;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;

import java.util.List;

/**
 * 租户库车队 base 重放 Runner（仅单体/基座应用装配，service-system 在 classpath）：应用就绪即把
 * classpath 全部 base 脚本（平台域 + 租户域，与 standalone 形态 BASE 通道同构全量）重放到租户库
 * 车队（共享主库 + 各独立租户库，各库自持副本与分册账本）。
 *
 * <p>排序紧随 {@code MasterSchemaAutoMigration}（+1000）与 {@code SharedMainBootstrap}（+1001，
 * 种缺省共享库登记行并 eager 建身份表——本 Runner 的目标枚举与 identityReady 判定依赖其就绪）之后、
 * 先于一切业务 Runner——菜单/权限副本就绪先于内置模板同步等启动期数据动作。失败不阻断启动
 * （脚本幂等 + 账本续跑，推送通道与下次启动继续收敛）；库级失败明细由重放器告警。
 */
public class BaseTenantFleetRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(BaseTenantFleetRunner.class);

    private final TenantMigrationScanner scanner;

    private final TenantFleetBaseReplayer fleetReplayer;

    public BaseTenantFleetRunner(TenantMigrationScanner scanner, TenantFleetBaseReplayer fleetReplayer) {
        this.scanner = scanner;
        this.fleetReplayer = fleetReplayer;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1002;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 三方言清单并集交给重放器按目标库方言筛取（脚本路径含 db/<方言>/ 段，账本键互不冲突）
        List<vip.mseapp.mybatis.schema.SchemaMigration> baseScripts = new java.util.ArrayList<>();
        for (String dialect : List.of("mysql", "dm", "kingbase")) {
            scanner.scan(dialect).stream()
                    .filter(m -> m.target() == MigrationTarget.BASE && !m.perTenant())
                    .forEach(baseScripts::add);
        }
        if (baseScripts.isEmpty()) {
            log.debug("[mse-base-fleet] classpath 无 base 脚本，车队重放闲置");
            return;
        }
        TenantFleetBaseReplayer.FleetResult result = fleetReplayer.replayAll(baseScripts);
        if (result.failed() > 0) {
            log.warn("[mse-base-fleet] 启动车队重放存在失败（推送通道/下次启动继续收敛）: {}", result.errors());
        } else if (result.applied() > 0 || result.targets() > 0) {
            log.info("[mse-base-fleet] 启动车队重放完成: 目标 {} 个、应用 {} 条、失败 {}",
                    result.targets(), result.applied(), result.failed());
        }
    }
}
