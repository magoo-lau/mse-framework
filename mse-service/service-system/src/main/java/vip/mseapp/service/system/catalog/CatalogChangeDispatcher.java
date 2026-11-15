package vip.mseapp.service.system.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.schedule.MseScheduled;
import vip.mseapp.mybatis.datasource.TenantFleetBaseReplayer;
import vip.mseapp.mybatis.schema.BaseScope;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.redis.MsePermCacheKeys;
import vip.mseapp.service.system.entity.SysCatalogChange;
import vip.mseapp.service.system.mapper.SysCatalogChangeMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录变更批次调谐器（controller 楼层）：周期把平台端目录编辑批次经
 * {@link TenantFleetBaseReplayer#replayAll} 扇出到登记库全集——账本按 script 键幂等
 * （biz=catalog、version=zeroPad10(id)、script=catalog/change/&lt;id&gt;），重放无副作用；
 * applied&gt;0 时递增权限缓存全局版本让两端鉴权回源。窗口内全量重放靠账本短路
 * （UI 编辑频次低），新租户库的首次收敛由懒初始化追平链（CatalogChangeSource）完成。
 *
 * <p>与推送通道的分工：脚本=声明式基线（启动一次推送摄入），批次=运行期编辑（本任务持续调谐）。
 */
public class CatalogChangeDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CatalogChangeDispatcher.class);

    private static final String CATALOG_BIZ = "catalog";

    private final SysCatalogChangeMapper mapper;

    private final TenantFleetBaseReplayer fleetReplayer;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private final int windowDays;

    public CatalogChangeDispatcher(SysCatalogChangeMapper mapper,
                                   TenantFleetBaseReplayer fleetReplayer,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                   int windowDays) {
        this.mapper = mapper;
        this.fleetReplayer = fleetReplayer;
        this.redisTemplateProvider = redisTemplateProvider;
        this.windowDays = Math.max(1, windowDays);
    }

    /** 周期扇出：窗口内批次 → SchemaMigration 清单 → 车队重放 → 失效权限缓存 → 标记已扇出 */
    @MseScheduled(taskId = "catalog-change-dispatch",
            name = "目录变更扇出",
            description = "平台端目录（菜单/权限点/子应用）编辑批次周期扇出到登记库全集（账本幂等），"
                    + "间隔 mse.catalog.dispatch.interval-seconds",
            fixedDelayString = "${mse.catalog.dispatch.interval-seconds:60}000",
            category = "platform")
    public void dispatch() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(windowDays);
        List<SysCatalogChange> pending = mapper.selectList(new LambdaQueryWrapper<SysCatalogChange>()
                .ge(SysCatalogChange::getCreateTime, cutoff)
                .orderByAsc(SysCatalogChange::getId));
        if (pending.isEmpty()) {
            return;
        }
        // 三方言变体：车队重放按 script 键的 db/<方言>/ 段筛方言（replayOn），目录 SQL 本身方言中立，
        // 每条批次展开为 catalog/change/<方言>/<id> 三个键，各目标库按自身方言命中、账本各记一行
        List<SchemaMigration> migrations = new ArrayList<>(pending.size() * DIALECTS.size());
        for (SysCatalogChange change : pending) {
            for (String dialect : DIALECTS) {
                migrations.add(toMigration(change, dialect));
            }
        }
        TenantFleetBaseReplayer.FleetResult result = fleetReplayer.replayAll(migrations);
        if (result.applied() > 0) {
            evictPermissionCache();
        }
        if (result.failed() > 0) {
            log.warn("[mse-catalog] 目录批次扇出存在失败（保持待扇出，下轮窗口重放补偿）: {}", result.errors());
        } else {
            log.info("[mse-catalog] 目录批次扇出: 窗口 {} 条、目标 {} 个、应用 {} 条",
                    pending.size(), result.targets(), result.applied());
            // 全部目标无失败才置已扇出（失败轮次保持待扇出，下轮重放靠账本幂等补偿）
            for (SysCatalogChange change : pending) {
                if (change.getStatus() == null || change.getStatus() == SysCatalogChange.STATUS_PENDING) {
                    SysCatalogChange mark = new SysCatalogChange();
                    mark.setId(change.getId());
                    mark.setStatus(SysCatalogChange.STATUS_DISPATCHED);
                    mapper.updateById(mark);
                }
            }
        }
    }

    private static final List<String> DIALECTS = List.of("mysql", "dm", "kingbase");

    private SchemaMigration toMigration(SysCatalogChange change, String dialect) {
        return new SchemaMigration(CATALOG_BIZ, String.format("%010d", change.getId()),
                "目录变更批次", SchemaMigration.TYPE_CLASSPATH, MigrationTarget.BASE, false,
                "catalog/change/" + dialect + "/" + change.getId(), change.getSqlText(),
                TenantMigrationScanner.checksum(change.getSqlText()), BaseScope.TENANT);
    }

    private void evictPermissionCache() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().increment(MsePermCacheKeys.globalVer());
        } catch (Exception e) {
            log.warn("[mse-catalog] 权限缓存版本递增失败（留待 TTL 兜底）: {}", e.getMessage());
        }
    }
}
