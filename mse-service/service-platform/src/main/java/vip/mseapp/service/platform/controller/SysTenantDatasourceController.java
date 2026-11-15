package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;
import vip.mseapp.service.platform.service.TenantDataMigrationService;

/**
 * 租户数据源管理（平台运营端；sys_tenant_datasource 为全局表，操作不受租户插件改写）。
 *
 * <p>租户库（共享库/独立库）一律经本界面维护并绑定租户。配置文件/Nacos 中的默认数据源
 * （spring.datasource.dynamic.primary，缺省 master）以内置行形式只读展示（单体=通用一条，
 * 微服务=通用 + 每个已知服务一条），可供共享租户绑定——运行时按共享库字段隔离语义落各服务
 * 各自的主库；内置行不可修改/删除/停用，页面操作一律只针对界面维护的租户库。
 */
@Validated
@RestController
@RequestMapping("/tenant/datasource")
public class SysTenantDatasourceController {

    private final SysTenantDatasourceService datasourceService;

    private final TenantDataMigrationService dataMigrationService;

    public SysTenantDatasourceController(SysTenantDatasourceService datasourceService,
                                         TenantDataMigrationService dataMigrationService) {
        this.datasourceService = datasourceService;
        this.dataMigrationService = dataMigrationService;
    }

    @SaCheckPermission(value = "tenant:ds:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/page")
    public Result<PageResult<SysTenantDatasourceService.DatasourceView>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String dsKey,
            @RequestParam(required = false) String dsMode,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) Integer status) {
        return Result.ok(datasourceService.pageDatasources(pageNum, pageSize, dsKey, dsMode, serviceName, status));
    }

    /** 下拉选项（租户开通/编辑的数据源绑定选择） */
    @SaCheckPermission(value = "tenant:ds:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(datasourceService.listOptions());
    }

    @SaCheckPermission(value = "tenant:ds:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}")
    public Result<SysTenantDatasourceService.DatasourceView> get(@PathVariable Long id) {
        return Result.ok(datasourceService.getDatasource(id));
    }

    @SaCheckPermission(value = "tenant:ds:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated SaveRequest request) {
        return Result.ok(datasourceService.createDatasource(request.toEntity()));
    }

    @SaCheckPermission(value = "tenant:ds:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated UpdateRequest request) {
        if (request.id() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "id 不能为空");
        }
        datasourceService.updateDatasource(request.toEntity());
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:ds:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        datasourceService.deleteDatasource(id);
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:ds:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        datasourceService.changeStatus(id, status);
        return Result.ok();
    }

    /** 连通测试：带 id 且表单无连接信息时按存量行测试；否则按表单（编辑页未保存场景） */
    @SaCheckPermission(value = "tenant:ds:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/test")
    public Result<Long> test(@RequestParam(required = false) Long id,
                             @RequestBody(required = false) SaveRequest request) {
        return Result.ok(datasourceService.testConnection(id, request == null ? null : request.toEntity()));
    }

    /** 初始化租户库（异步）：建库 + 三通道全量版本化脚本直建提交后台执行，校验通过即返回；状态经 GET /{id}/init-status 轮询 */
    @SaCheckPermission(value = "tenant:ds:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/init")
    public Result<String> init(@PathVariable Long id) {
        return Result.ok(datasourceService.initDatabase(id));
    }

    /** 初始化任务状态（轮询通道）：status NULL 未发起 / 0 初始化中 / 1 成功 / 2 失败（message=摘要或失败原因） */
    @SaCheckPermission(value = "tenant:ds:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}/init-status")
    public Result<SysTenantDatasourceService.InitStatusView> initStatus(@PathVariable Long id) {
        return Result.ok(datasourceService.initStatus(id));
    }

    /** 对该租户库应用 pending 版本化迁移脚本（存量库自动按主库当前版本打基线） */
    @SaCheckPermission(value = "tenant:ds:migrate", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/migrate")
    public Result<String> migrate(@PathVariable Long id) {
        return Result.ok(datasourceService.migrate(id));
    }

    /** 迁移预演（只读）：TENANT/MASTER/BASE 三通道「将要执行的脚本清单」+ 冲突 + 基线判定，不执行任何语句 */
    @SaCheckPermission(value = "tenant:ds:sync", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}/schema-plan")
    public Result<SysTenantDatasourceService.SchemaPlanView> schemaPlan(@PathVariable Long id) {
        return Result.ok(datasourceService.planSchema(id));
    }

    /** 迁移 + 结构比对补缺（缺表/缺列/缺索引自动补，漂移只报告；比对仅 MySQL） */
    @SaCheckPermission(value = "tenant:ds:sync", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/sync-schema")
    public Result<String> syncSchema(@PathVariable Long id) {
        return Result.ok(datasourceService.syncSchema(id));
    }

    /** 对全部启用数据源批量同步（异步）：登记批次即返回，逐库执行失败不中断；进度经 GET /batch-sync/status 轮询 */
    @SaCheckPermission(value = "tenant:ds:sync", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/batch-sync")
    public Result<String> batchSync() {
        return Result.ok(datasourceService.batchSync());
    }

    /** 批量同步批次进度（轮询通道）：running/total/done/failed + 逐库最新状态（dsKey 对应列表行） */
    @SaCheckPermission(value = "tenant:ds:sync", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/batch-sync/status")
    public Result<SysTenantDatasourceService.BatchSyncStatusView> batchSyncStatus() {
        return Result.ok(datasourceService.batchSyncStatus());
    }

    /** 结构状态：当前版本/基线/pending 脚本/checksum 冲突/最近失败 */
    @SaCheckPermission(value = "tenant:ds:sync", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}/schema-status")
    public Result<SysTenantDatasourceService.SchemaStatusView> schemaStatus(@PathVariable Long id) {
        return Result.ok(datasourceService.schemaStatus(id));
    }

    /** 界面临时 SQL：直接在该租户库执行并记 ADHOC 历史流水 */
    @SaCheckPermission(value = "tenant:ds:execute", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/execute-sql")
    public Result<String> executeSql(@PathVariable Long id, @RequestBody @Validated AdhocSqlRequest request) {
        return Result.ok(datasourceService.executeAdhoc(id, request.sql(), request.description()));
    }

    /** 界面临时 SQL 请求 */
    public record AdhocSqlRequest(@NotBlank(message = "SQL 不能为空") String sql, @Size(max = 200) String description) {
    }

    /** 新增请求（密码必填） */
    public record SaveRequest(
            @NotBlank(message = "数据源 key 不能为空") @Size(max = 60) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "数据源 key 仅允许字母/数字/下划线/中划线") String dsKey,
            @NotBlank(message = "库模式不能为空") @Pattern(regexp = "shared|dedicated", message = "库模式仅允许 shared/dedicated") String dsMode,
            @NotBlank(message = "所属服务不能为空") @Size(max = 30) String serviceName,
            @NotBlank(message = "数据库类型不能为空") @Pattern(regexp = "mysql|dm|kingbase", message = "数据库类型仅允许 mysql/dm/kingbase") String dbType,
            @NotBlank(message = "主机不能为空") @Size(max = 128) String host,
            Integer port,
            @NotBlank(message = "库名不能为空") @Size(max = 64) String dbName,
            @Size(max = 500) String params,
            @NotBlank(message = "连接账号不能为空") @Size(max = 64) String username,
            String password,
            @Size(max = 255) String remark) {

        SysTenantDatasource toEntity() {
            SysTenantDatasource ds = new SysTenantDatasource();
            ds.setDsKey(dsKey);
            ds.setDsMode(dsMode);
            ds.setServiceName(serviceName.trim());
            ds.setDbType(dbType);
            ds.setHost(host);
            ds.setPort(port);
            ds.setDbName(dbName);
            ds.setParams(params);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setRemark(remark);
            return ds;
        }
    }

    /** 修改请求（id 必填；密码留空保持原值） */
    public record UpdateRequest(
            Long id,
            @NotBlank(message = "数据源 key 不能为空") @Size(max = 60) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "数据源 key 仅允许字母/数字/下划线/中划线") String dsKey,
            @NotBlank(message = "库模式不能为空") @Pattern(regexp = "shared|dedicated", message = "库模式仅允许 shared/dedicated") String dsMode,
            @NotBlank(message = "所属服务不能为空") @Size(max = 30) String serviceName,
            @NotBlank(message = "数据库类型不能为空") @Pattern(regexp = "mysql|dm|kingbase", message = "数据库类型仅允许 mysql/dm/kingbase") String dbType,
            @NotBlank(message = "主机不能为空") @Size(max = 128) String host,
            Integer port,
            @NotBlank(message = "库名不能为空") @Size(max = 64) String dbName,
            @Size(max = 500) String params,
            @NotBlank(message = "连接账号不能为空") @Size(max = 64) String username,
            String password,
            @Size(max = 255) String remark) {

        SysTenantDatasource toEntity() {
            SysTenantDatasource ds = new SysTenantDatasource();
            ds.setId(id);
            ds.setDsKey(dsKey);
            ds.setDsMode(dsMode);
            ds.setServiceName(serviceName.trim());
            ds.setDbType(dbType);
            ds.setHost(host);
            ds.setPort(port);
            ds.setDbName(dbName);
            ds.setParams(params);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setRemark(remark);
            return ds;
        }
    }

    /** 租户换库搬迁：把该租户在当前绑定库/平台库的行搬到目标数据源（绑定变更后执行） */
    @SaCheckPermission(value = "tenant:ds:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/data-migrate/tenant/{tenantId}")
    public Result<String> migrateTenantRows(@PathVariable Long tenantId,
                                            @RequestParam @NotBlank String toDsKey) {
        return Result.ok(dataMigrationService.migrateTenant(tenantId, toDsKey));
    }
}
