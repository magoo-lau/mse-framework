package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.platform.entity.SysTenantDatasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户数据源管理（界面化维护的租户库：共享库/独立库）。
 *
 * <p>增删改会即时应用到本进程的 dynamic-datasource 注册表并经 Redis 广播到全部服务进程；
 * 租户绑定校验（dedicated 独占、shared 不限）供 {@link SysTenantService} 开通/编辑租户时调用。
 */
public interface SysTenantDatasourceService extends IRepository<SysTenantDatasource> {

    PageResult<DatasourceView> pageDatasources(long pageNum, long pageSize, String dsKey, String dsMode, String serviceName, Integer status);

    /** 下拉/列表选项（仅启用行 + 内置主库虚拟行），绑定信息用于 dedicated 独占校验的前端提示 */
    List<DatasourceView> listOptions();

    /** 详情（密码不回显） */
    DatasourceView getDatasource(Long id);

    /** 新增：保存 + 本进程注册 + 广播 */
    Long createDatasource(SysTenantDatasource datasource);

    /** 修改：密码留空保持原值；dsKey 已被租户绑定时不可改 + 本进程重注册 + 广播 */
    void updateDatasource(SysTenantDatasource datasource);

    /** 删除：被租户绑定时拒绝 */
    void deleteDatasource(Long id);

    /** 启用/停用：被租户绑定时拒绝停用（停用即注销运行时数据源，路由会回落平台库） */
    void changeStatus(Long id, Integer status);

    /** 连通测试：id 非空按存量行（password 空则用库中密文），否则按临时表单；返回耗时毫秒 */
    long testConnection(Long id, SysTenantDatasource form);

    /**
     * 初始化租户库（异步）：建库（若不存在）+「TENANT 身份表 + MASTER 业务表 + BASE base 副本」
     * 三通道全量脚本直建提交后台单线程执行，校验通过即返回（耗时随业务积木增长且不可控，
     * 同步执行必撞前端超时；同 {@link TenantProvisioner} 先例，状态落行供轮询）。
     * 同一数据源任务进行中拒绝重复提交。
     */
    String initDatabase(Long id);

    /** 初始化任务状态查询（轮询通道）：status NULL 未发起 / 0 初始化中 / 1 成功 / 2 失败 */
    InitStatusView initStatus(Long id);

    /** 对该租户库应用 pending 版本化迁移脚本（存量库自动按主库当前版本打基线），返回执行摘要 */
    String migrate(Long id);

    /** 迁移预演（只读）：TENANT/MASTER/BASE 三通道「将要执行的脚本清单」+ 冲突 + 基线判定说明，不执行任何语句 */
    SchemaPlanView planSchema(Long id);

    /** 迁移 + 结构比对补缺（缺表/缺列/缺索引自动补，漂移只报告；比对仅 MySQL），返回综合报告；结果落行 sync_* 供列表展示 */
    String syncSchema(Long id);

    /**
     * 对全部启用数据源批量同步（异步）：登记批次即返回，后台单线程逐库执行
     * （迁移 + 结构比对，复用单库 {@link #syncSchema}，失败不中断），进度与逐库结果经
     * {@link #batchSyncStatus()} 轮询；同一时刻至多一个批次。
     */
    String batchSync();

    /** 批量同步批次进度（轮询通道；Redis 缺席时仅本实例可见） */
    BatchSyncStatusView batchSyncStatus();

    /** 结构状态：当前版本/基线/pending 脚本/checksum 冲突/最近失败 */
    SchemaStatusView schemaStatus(Long id);

    /** 界面临时 SQL：直接在该租户库执行并记 ADHOC 历史流水（审计可查） */
    String executeAdhoc(Long id, String sql, String description);

    /** 迁移预演单通道视图（展示口径：pending/conflicts/notes 均为可直读字符串） */
    record ChannelPlanView(String channel, int pendingCount, List<String> pending,
                           List<String> conflicts, List<String> notes) {
    }

    /** 迁移预演视图（三通道：身份结构 TENANT / 业务结构 MASTER / base 副本 BASE） */
    record SchemaPlanView(String dbType, List<ChannelPlanView> channels) {
    }

    /** 批量同步逐库进度项：status 0 同步中 / 1 成功 / 2 失败 */
    record BatchSyncItemView(String dsKey, String dbName, String dbType, Integer status, String message) {
    }

    /** 批量同步批次进度（running=批次执行中；items 为逐库最新状态） */
    record BatchSyncStatusView(boolean running, int total, int done, int failed, List<BatchSyncItemView> items) {
    }

    /** 初始化任务状态视图（轮询通道；message=成功摘要或失败原因） */
    record InitStatusView(Integer status, String message) {
    }

    /** 结构状态视图 */
    record SchemaStatusView(String dbType, boolean historyReady, String currentVersion, String baselineVersion,
                            List<String> pending, List<String> conflicts, String lastError) {
    }

    /**
     * 展示视图：行数据（不含密码密文）+ 绑定统计 + 本进程运行状态。
     * {@code taken} = dedicated 且已被绑定（下拉中不再可被其他租户选择）。
     *
     * <p>内置行（{@code builtIn=true}）：配置文件/Nacos 默认主库（spring.datasource.dynamic.primary，
     * 缺省 master）的虚拟行，不落 sys_tenant_datasource——通用行（serviceName='*'，单体=本进程主库坐标，
     * 微服务=各服务各自的主库）+ 微服务形态每个已知服务一行；id 为哨兵值（0/负数），界面不可修改。
     * {@code registered} 三态：true/false=本进程注册状态，null=远端服务内置行（本进程无从判定）。
     */
    record DatasourceView(Long id, String dsKey, String dsMode, String serviceName, String dbType, String host,
                          Integer port, String dbName, String params, String username, Integer status, String remark,
                          LocalDateTime createTime,
                          long boundTenants, String boundTenantNames, boolean taken, Boolean registered,
                          Integer initStatus, String initMessage, Integer syncStatus, String syncMessage,
                          java.time.LocalDateTime syncTime, boolean builtIn) {
    }

}
