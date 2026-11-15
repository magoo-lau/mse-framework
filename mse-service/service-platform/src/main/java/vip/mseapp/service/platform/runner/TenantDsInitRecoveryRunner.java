package vip.mseapp.service.platform.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户数据源初始化任务的对账恢复器：进程重启/崩溃会把排队中的后台初始化任务连同执行器一起
 * 丢弃（shutdownNow 丢队列任务），行状态会永久停留「初始化中」（init_status=0）——本 Runner
 * 启动时扫描<b>超时仍进行中</b>的行重新提交（幂等：CREATE DATABASE IF NOT EXISTS + 账本续跑，
 * 重新提交即刷新 init_started_at）。
 *
 * <p>超时阈值 &gt; 执行期锁 TTL（15min）+ 缓冲：阈值内视为他实例正常执行中不抢（重新提交
 * 会把好任务的发起时间刷新、状态被本轮实例误覆盖）。执行期统一锁兜底最终互斥——即便误判
 * 重提，后到者在锁上让位并标失败，不会并发跑脚本。
 */
public class TenantDsInitRecoveryRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantDsInitRecoveryRunner.class);

    /** 恢复判定阈值：执行期锁 TTL（15min）+ 5min 缓冲 */
    private static final int RECOVERY_AFTER_MINUTES = 20;

    private final SysTenantDatasourceMapper datasourceMapper;

    private final SysTenantDatasourceService datasourceService;

    public TenantDsInitRecoveryRunner(SysTenantDatasourceMapper datasourceMapper,
                                      SysTenantDatasourceService datasourceService) {
        this.datasourceMapper = datasourceMapper;
        this.datasourceService = datasourceService;
    }

    /** 晚于启动结构迁移（+1000）与车队重放（+1002）：init_status/init_started_at 列就绪后再对账 */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3000;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RECOVERY_AFTER_MINUTES);
        List<SysTenantDatasource> stuck = datasourceMapper.selectList(new LambdaQueryWrapper<SysTenantDatasource>()
                .eq(SysTenantDatasource::getInitStatus, 0)
                .and(wrapper -> wrapper.isNull(SysTenantDatasource::getInitStartedAt)
                        .or().lt(SysTenantDatasource::getInitStartedAt, cutoff)));
        if (stuck.isEmpty()) {
            return;
        }
        for (SysTenantDatasource ds : stuck) {
            try {
                log.warn("[mse-tenant-ds] 检测到悬挂的初始化任务（实例重启丢失），自动重新提交: id={} key={} startedAt={}",
                        ds.getId(), ds.getDsKey(), ds.getInitStartedAt());
                datasourceService.initDatabase(ds.getId());
            } catch (Exception e) {
                // 单行失败不阻断其余恢复；initDatabase 内部已把该行标失败，管理端可见可人工重提
                log.warn("[mse-tenant-ds] 悬挂初始化任务恢复提交失败: id={} key={} {}", ds.getId(), ds.getDsKey(), e.getMessage());
            }
        }
    }
}
