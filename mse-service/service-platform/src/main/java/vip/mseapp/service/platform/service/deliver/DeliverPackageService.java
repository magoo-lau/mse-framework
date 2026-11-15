package vip.mseapp.service.platform.service.deliver;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.license.DeliverCodec;
import vip.mseapp.core.license.LicenseCodec;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.datasource.TenantDatasourceDefinition;
import vip.mseapp.mybatis.datasource.TenantDbCloner;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.platform.entity.SysDeliverPackage;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.mapper.SysDeliverPackageMapper;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;

import javax.sql.DataSource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 交付附件签发门面（平台侧，三段管道的「签发」段，deliver-design.md ADR-9 职责归位）：
 * 平台<b>只签发「交付附件包」（attachments format:1）</b>——license 凭证 + 租户数据 JSONL +
 * 租户变量 vars.env + extras/schema.sql；最终交付包由 deliver-studio「套件 + 附件」重打包产出，
 * 平台不再装配套件 zip、不再重建外层清单（历史职责已移交 studio delivery.go）。
 *
 * <p>附件包结构（studio 侧 POST /api/deliveries 全量校验后入库）：
 * <pre>
 * attachments.json          描述符：format:1/tenant/licenseId/packageType/modules/requires/files(逐文件sha256)
 * delivery/vars.env         租户身份 + license 公钥（console 表单自动预填）
 * delivery/deploy-package.zip RS256 签名数据载荷（license.token + data/*.jsonl + manifest formatVersion:1
 *                           + signature.sig——字节格式与导入端 DeliverImportService 约定不动，零改动）
 * extras/schema.sql         租户库表结构（幂等 DDL，MySQL 首启 initdb 预置）
 * </pre>
 *
 * <p>流转双通道：附件包归档（记录行可再下载，手动上传 studio）或直推 deliver-studio
 * （mse.deliver.studio.{url,user,password} 配置；推送失败不影响签发成功）。
 * 签发为异步任务：记录行即任务（status/progress/stage），前端按记录轮询进度。
 */
public class DeliverPackageService
        extends com.baomidou.mybatisplus.spring.repository.CrudRepository<SysDeliverPackageMapper, SysDeliverPackage> {

    private static final Logger log = LoggerFactory.getLogger(DeliverPackageService.class);

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /** 单区间 Range 头（bytes=start-end / bytes=start- / bytes=-suffix）；不匹配的多区间等罕见形态退化为整包 200 */
    private static final Pattern RANGE_HEADER = Pattern.compile("^bytes=(\\d*)-(\\d*)$");

    /** manifest 规范化序列化（键排序，导入端可重建同一字节）；Jackson 2（版本由 Boot 4 的 jackson-2-bom 托管）——
     *  签名验证按字节比对，与导入端 DeliverImportService 必须同 mapper 家族，如切 Jackson 3 需两端成对切 */
    private static final ObjectMapper CANONICAL = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final SysDeliverPackageMapper packageMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantDatasourceMapper datasourceMapper;
    private final ObjectProvider<DataSource> masterDataSourceProvider;
    private final ObjectProvider<MseTenantProperties> tenantPropertiesProvider;
    private final String privateKey;
    private final String archiveDir;
    private final String platformVersion;
    private final long maxTableRows;

    private final TenantDataExporter exporter;

    /** deliver-studio 直推客户端（mse.deliver.studio.url 未配置时为 null，仅手动下载通道） */
    private final StudioPushClient studioPush;

    /** 同时处于「签发中」（执行中+排队）的任务上限：超限拒绝新提交，防止前端批量请求压垮服务/塞满磁盘 */
    private final int maxActiveTasks;

    /** 进行中任务的租户集合（原子去重：同租户仅允许一个签发任务） */
    private final Set<Long> inFlightTenants = ConcurrentHashMap.newKeySet();

    /** 下载签名密钥（HMAC-SHA256）：浏览器原生下载带不了 Authorization 头，凭短时签名 URL 免会话直下 */
    private final byte[] downloadSignKey;

    /** 签名 URL 有效期（秒）：期间内 Range 重试/断点续传均有效，过期须回页面重新签发 */
    private final long downloadUrlTtlSeconds;

    /** 单线程串行执行 + 有界队列（容量=上限-1），队列满以 RejectedExecutionException 原子拒绝 */
    private final ExecutorService exportExecutor;

    public DeliverPackageService(SysDeliverPackageMapper packageMapper,
                                 SysTenantMapper tenantMapper,
                                 SysTenantDatasourceMapper datasourceMapper,
                                 ObjectProvider<DataSource> masterDataSourceProvider,
                                 ObjectProvider<MseTenantProperties> tenantPropertiesProvider,
                                 String privateKey,
                                 String archiveDir,
                                 String platformVersion,
                                 long maxTableRows,
                                 int maxActiveTasks,
                                 String downloadSignKey,
                                 long downloadUrlTtlSeconds,
                                 StudioPushClient studioPush) {
        this.packageMapper = packageMapper;
        this.tenantMapper = tenantMapper;
        this.datasourceMapper = datasourceMapper;
        this.masterDataSourceProvider = masterDataSourceProvider;
        this.tenantPropertiesProvider = tenantPropertiesProvider;
        this.privateKey = privateKey;
        this.archiveDir = archiveDir == null || archiveDir.isBlank() ? "./data/deliver" : archiveDir;
        this.platformVersion = platformVersion == null ? "" : platformVersion;
        this.maxTableRows = maxTableRows;
        this.maxActiveTasks = Math.max(1, maxActiveTasks);
        this.downloadUrlTtlSeconds = Math.max(60, downloadUrlTtlSeconds);
        this.studioPush = studioPush;
        if (downloadSignKey == null || downloadSignKey.isBlank()) {
            // 未配置则每次启动随机生成：重启后已签发 URL 全部失效（TTL 本就分钟级，可接受）；
            // 多实例部署须配置同一 mse.deliver.download-sign-key，否则 URL 落到别的副本验签失败
            byte[] ephemeral = new byte[32];
            new SecureRandom().nextBytes(ephemeral);
            this.downloadSignKey = ephemeral;
            log.info("[mse-deliver] 下载签名密钥未配置，已生成进程内临时密钥（重启使已签发下载 URL 失效）");
        } else {
            this.downloadSignKey = downloadSignKey.getBytes(StandardCharsets.UTF_8);
        }
        this.exporter = new TenantDataExporter(maxTableRows);
        this.exportExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(this.maxActiveTasks - 1),
                r -> {
                    Thread thread = new Thread(r, "mse-deliver-export");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 启动自愈：进程重启会丢失 executor 队列中的排队任务，其记录会永远停留在「签发中」——
     * 启动时统一标记为失败（请重新提交）。交付中心按单实例部署；多实例共库时此清理会误伤
     * 其他实例执行中的任务，属已知边界。
     *
     * <p>经统一调度中心执行（startup 任务）：应用就绪 + 延迟启动窗口后才跑，确保平台库
     * 结构迁移已完成；多实例部署时执行互斥保证仅一实例清理。
     */
    @vip.mseapp.core.schedule.MseScheduled(taskId = "deliver-startup-cleanup", name = "交付中断任务启动清理",
            description = "重启后把停留在「签发中」的中断任务统一标记失败", startup = true,
            category = "platform")
    void cleanupInterruptedTasks() {
        try {
            int reset = packageMapper.update(null, new LambdaUpdateWrapper<SysDeliverPackage>()
                    .eq(SysDeliverPackage::getStatus, SysDeliverPackage.STATUS_RUNNING)
                    .set(SysDeliverPackage::getStatus, SysDeliverPackage.STATUS_FAILED)
                    .set(SysDeliverPackage::getErrorMsg, "服务重启导致签发中断，请重新提交")
                    .set(SysDeliverPackage::getStage, "失败"));
            if (reset > 0) {
                log.warn("[mse-deliver] 启动清理：{} 个中断的签发任务已标记失败", reset);
            }
        } catch (Exception e) {
            log.warn("[mse-deliver] 启动清理中断任务失败（表未就绪？）: {}", e.getMessage());
        }
    }

    // ===== 请求/视图 =====

    public record ExportRequest(Long tenantId, String packageType, Boolean includeData,
                                String licenseId, String licenseToken,
                                List<String> modules, Boolean pushStudio,
                                List<String> patchTables, String remark) {
    }

    public record PackageView(Long id, Long tenantId, String tenantCode, String packageType, String licenseId,
                              String moduleList, String studioDeliveryId, String pushStatus, Integer sharedMode,
                              Integer status, Integer progress, String stage, String errorMsg,
                              Integer tableCount, Long rowCount, String fileName,
                              Long fileSize, String fileSha256, String remark, LocalDateTime createTime) {
    }

    /** deliver-studio 直推配置状态（签发对话框展示推送选项用） */
    public record StudioInfo(boolean enabled, String url) {
    }

    /** 签名下载 URL（url 为应用内相对路径，前端拼 /api 前缀交浏览器原生下载；expireAt=epoch 毫秒） */
    public record DownloadUrlView(String url, long expireAt) {
    }

    /** 已导出表（manifest.tables 行） */
    record ExportedTable(String table, String file, long rows, long size, String sha256) {
    }

    /** 包内清单项（manifest.files 行） */
    record PackagedFile(String file, long size, String sha256) {
    }

    // ===== 附件签发（异步任务） =====

    /**
     * 提交签发任务：同步前置校验 + 落「签发中」记录即返回，后台产出附件包（可选直推 studio）。
     * 记录行即任务——前端按记录轮询 status/progress/stage。
     */
    public PackageView submitExport(ExportRequest request) {
        SysTenant tenant = request.tenantId() == null ? null : tenantMapper.selectById(request.tenantId());
        if (tenant == null) {
            throw new ServiceException(404, "租户不存在");
        }
        boolean full = !SysDeliverPackage.TYPE_UPGRADE.equalsIgnoreCase(request.packageType());
        boolean includeData = request.includeData() == null || request.includeData();
        if (full && includeData && (isBlank(request.licenseToken()) || isBlank(request.licenseId()))) {
            throw new ServiceException(400, "全量附件包须随包签发授权凭证（先调用 /tenant/license/issue）");
        }
        if (isBlank(privateKey)) {
            throw new ServiceException(400, "平台未配置签发私钥（mse.license.private-key），无法签名附件包");
        }
        if (Boolean.TRUE.equals(request.pushStudio()) && (studioPush == null || !studioPush.enabled())) {
            throw new ServiceException(400, "未配置 deliver-studio 直推（mse.deliver.studio.url/user/password），请改用手动下载上传");
        }
        // 升级包补丁表同步校验：此前校验在异步任务体，接口 code=0 落 RUNNING 后才异步失败（契约与文档不符）
        validatePatchTablesSync(tenant, request, full);

        // 兜底护栏：正常由 inFlight/有界队列先行拒绝（并行提交竞态下 DB 积压异常时提前止损）
        Long active = packageMapper.selectCount(new LambdaQueryWrapper<SysDeliverPackage>()
                .eq(SysDeliverPackage::getStatus, SysDeliverPackage.STATUS_RUNNING));
        if (active != null && active >= maxActiveTasks * 4L) {
            throw new ServiceException(429, "签发任务积压（" + active + " 个），请稍后再试");
        }

        if (!inFlightTenants.add(tenant.getId())) {
            throw new ServiceException(429, "该租户已有签发任务进行中，请等待完成后再次提交");
        }

        String type = full ? SysDeliverPackage.TYPE_FULL : SysDeliverPackage.TYPE_UPGRADE;
        // 归档名带 6 位随机后缀：原 yyyyMMddHHmm 粒度下同分钟同租户同类型两包互覆
        String fileName = "attachments-" + tenant.getCode() + "-" + type + "-"
                + LocalDateTime.now().format(FILE_TIME) + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 6) + ".zip";
        Path archive = Path.of(archiveDir, fileName);

        SysDeliverPackage record = new SysDeliverPackage();
        record.setTenantId(tenant.getId());
        record.setTenantCode(tenant.getCode());
        record.setPackageType(type);
        record.setLicenseId(request.licenseId());
        record.setModuleList(modulesCsv(request.modules()));
        record.setSharedMode(0);
        record.setStatus(SysDeliverPackage.STATUS_RUNNING);
        record.setProgress(0);
        record.setStage("排队中");
        record.setTableCount(0);
        record.setRowCount(0L);
        record.setFileName(fileName);
        record.setFilePath(archive.toString());
        record.setFileSize(0L);
        record.setFileSha256("");
        record.setStudioDeliveryId("");
        record.setPushStatus(Boolean.TRUE.equals(request.pushStudio()) ? "pending" : "");
        record.setRemark(isBlank(request.remark()) ? null : request.remark().trim());
        save(record);

        final SysDeliverPackage taskRecord = record;
        final SysTenant taskTenant = tenant;
        boolean submitted = false;
        try {
            exportExecutor.submit(() -> {
                try {
                    runExport(taskRecord, request, taskTenant, full, includeData, archive);
                } catch (Exception e) {
                    log.error("[mse-deliver] 附件签发失败: tenant={} file={}", taskTenant.getCode(), fileName, e);
                    markFailed(taskRecord, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                    deleteQuietly(archive);
                } finally {
                    inFlightTenants.remove(tenant.getId());
                }
            });
            submitted = true;
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // 队列满（进行中达上限）：删除刚落的记录并拒绝
            packageMapper.deleteById(record.getId());
            throw new ServiceException(429, "签发队列已满（进行中上限 " + maxActiveTasks + "），请稍后再试");
        } finally {
            if (!submitted) {
                inFlightTenants.remove(tenant.getId());
            }
        }
        return toView(record);
    }

    /**
     * 任务体：租户库收集 → <b>内层数据包</b>（license + 数据 JSONL，RS256 签名，导入端消费，格式不动）→
     * <b>附件包</b>（attachments.json 描述符 + delivery/vars.env + delivery/deploy-package.zip +
     * extras/schema.sql）→ 归档（可选直推 deliver-studio）。
     */
    private void runExport(SysDeliverPackage record, ExportRequest request, SysTenant tenant,
                           boolean full, boolean includeData, Path archive) throws Exception {
        SysTenantDatasource ds = resolveDatasource(tenant);
        progress(record, 1, "连接租户库");
        try (Connection conn = openTenantConnection(tenant, ds)) {
            String dbType = TenantSchemaMigrator.detectDbType(conn);
            if (!"mysql".equals(dbType)) {
                throw new ServiceException(400, "v1 仅支持 MySQL 租户库导出（当前: " + dbType + "）");
            }
            boolean shared = isSharedMode(ds);
            Set<String> ignoreTables = TenantDbCloner.ignoreTableNames(
                    tenantPropertiesProvider.getIfAvailable(MseTenantProperties::new));
            progress(record, 3, "清点表清单与账本基线");
            List<String> tables = exporter.exportTables(conn, shared, ignoreTables);
            if (!full) {
                tables = patchTables(tables, request.patchTables());
            }
            // 按签发模块集过滤数据表（模块子集裁剪：未选积木的表不进包，requires 亦不含）
            Set<String> moduleFilter = moduleFilterOf(request.modules());
            Map<String, String> tableBiz = tableBizByScript(dbType);
            Map<String, String> requires = exporter.ledgerVersions(conn);
            if (moduleFilter != null) {
                tables.removeIf(t -> {
                    String biz = tableBiz.get(t.toLowerCase(Locale.ROOT));
                    // 基座模块（system）的租户域数据表（如 sys_dict_data）随包保留
                    return biz == null || (moduleFilter.contains(biz) == false && "system".equals(biz) == false);
                });
                requires.keySet().removeIf(biz -> moduleFilter.contains(biz) == false
                        && "system".equals(biz) == false && "deliver".equals(biz) == false);
            }

            // ---- 内层数据包（delivery/deploy-package.zip）：license + 数据 JSONL + RS256 签名 ----
            progress(record, 4, "生成部署数据包（数据 + 凭证 + 签名）");
            Path dataZip = Files.createTempFile(Path.of(archiveDir), "deliver-data-", ".zip");
            List<ExportedTable> exportedTables = new ArrayList<>();
            List<PackagedFile> auxFiles = new ArrayList<>();
            String manifestJson;
            try {
                PackageWriter inner = PackageWriter.create(dataZip);
                try {
                    if (!isBlank(request.licenseToken())) {
                        byte[] token = request.licenseToken().getBytes(StandardCharsets.UTF_8);
                        inner.putBytes("license.token", token);
                        auxFiles.add(new PackagedFile("license.token", token.length, TenantDataExporter.sha256(token)));
                    }
                    if (includeData || !full) {
                        final List<String> exportList = List.copyOf(tables);
                        int index = 0;
                        for (String table : exportList) {
                            progress(record, 5 + (int) (50.0 * index / Math.max(1, exportList.size())),
                                    "导出数据 " + table);
                            exportedTables.add(exportTable(inner, conn, table, shared, tenant.getId()));
                            index++;
                        }
                    }
                    Map<String, Object> manifest = buildManifest(record.getPackageType(), tenant, shared, request,
                            requires, exportedTables, auxFiles);
                    byte[] canonical = CANONICAL.writeValueAsBytes(manifest);
                    inner.finishManifest(canonical, DeliverCodec.sign(canonical, privateKey));
                    manifestJson = new String(canonical, StandardCharsets.UTF_8);
                } finally {
                    closeQuietly(inner);
                }
            } catch (Exception e) {
                deleteQuietly(dataZip);
                throw e;
            }

            // ---- 附件包（attachments format:1）----
            try {
                progress(record, 80, "装配附件包");
                writeAttachmentsBundle(archive, record.getPackageType(), tenant,
                        isBlank(request.licenseId()) ? "" : request.licenseId(),
                        modulesCsv(request.modules()), requires, platformVersion,
                        varsEnv(tenant), schemaSql(conn, tables), dataZip);
                record.setSharedMode(shared ? 1 : 0);
                record.setTableCount(exportedTables.size());
                record.setRowCount(exportedTables.stream().mapToLong(ExportedTable::rows).sum());
                record.setFileSize(Files.size(archive));
                record.setFileSha256(TenantDataExporter.sha256(Files.readAllBytes(archive)));
                record.setManifestJson(manifestJson);
                record.setStatus(SysDeliverPackage.STATUS_DONE);
                record.setProgress(100);
                record.setStage("完成");
                updateById(record);
                log.info("[mse-deliver] 附件包签发完成: tenant={}({}) type={} tables={} rows={} size={} file={}",
                        tenant.getCode(), tenant.getId(), record.getPackageType(),
                        exportedTables.size(), record.getRowCount(), record.getFileSize(), record.getFileName());
            } catch (Exception e) {
                deleteQuietly(dataZip);
                throw e;
            }
            deleteQuietly(dataZip);
        }

        // ---- 可选直推 deliver-studio（失败不影响签发成功，手动下载兜底）----
        if (Boolean.TRUE.equals(request.pushStudio()) && studioPush != null && studioPush.enabled()) {
            progress(record, 99, "直推 deliver-studio");
            try {
                long deliveryId = studioPush.push(archive, record.getFileName(), tenant.getCode());
                packageMapper.update(null, new LambdaUpdateWrapper<SysDeliverPackage>()
                        .eq(SysDeliverPackage::getId, record.getId())
                        .set(SysDeliverPackage::getStudioDeliveryId, String.valueOf(deliveryId))
                        .set(SysDeliverPackage::getPushStatus, "pushed"));
                log.info("[mse-deliver] 附件包已直推 deliver-studio: tenant={} studioDeliveryId={}",
                        tenant.getCode(), deliveryId);
            } catch (Exception e) {
                String msg = "failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                packageMapper.update(null, new LambdaUpdateWrapper<SysDeliverPackage>()
                        .eq(SysDeliverPackage::getId, record.getId())
                        .set(SysDeliverPackage::getPushStatus, msg));
                log.warn("[mse-deliver] 附件包直推 deliver-studio 失败（附件包已归档，可手动上传）: tenant={} {}",
                        tenant.getCode(), msg);
            }
        }
    }

    /**
     * 附件包写出（attachments format:1，studio 侧 POST /api/deliveries 校验合同）：
     * attachments.json 描述符（files=除描述符外逐文件 sha256）+ delivery/vars.env +
     * delivery/deploy-package.zip（原字节拷入）+ extras/schema.sql。
     */
    static void writeAttachmentsBundle(Path out, String packageType, SysTenant tenant, String licenseId,
                                       String modulesCsv, Map<String, String> requires, String platformVersion,
                                       String varsEnv, String schemaSql, Path dataZip) throws IOException {
        byte[] varsBytes = varsEnv.getBytes(StandardCharsets.UTF_8);
        byte[] schemaBytes = schemaSql.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = Files.readAllBytes(dataZip);
        Map<String, String> files = new TreeMap<>();
        files.put("delivery/vars.env", TenantDataExporter.sha256(varsBytes));
        files.put("delivery/deploy-package.zip", TenantDataExporter.sha256(dataBytes));
        files.put("extras/schema.sql", TenantDataExporter.sha256(schemaBytes));

        Map<String, Object> tenantNode = new LinkedHashMap<>();
        tenantNode.put("id", tenant.getId());
        tenantNode.put("code", tenant.getCode());
        tenantNode.put("name", tenant.getName());
        Map<String, Object> requiresNode = new LinkedHashMap<>();
        requiresNode.put("platformVersion", platformVersion == null ? "" : platformVersion);
        requiresNode.put("perBizSchemaVersion", new LinkedHashMap<>(requires));
        Map<String, Object> decl = new LinkedHashMap<>();
        decl.put("format", 1);
        decl.put("tenant", tenantNode);
        decl.put("licenseId", licenseId == null ? "" : licenseId);
        decl.put("packageType", packageType);
        List<String> modules = new ArrayList<>();
        if (modulesCsv != null && !modulesCsv.isBlank()) {
            for (String m : modulesCsv.split(",")) {
                if (!m.isBlank()) {
                    modules.add(m.trim());
                }
            }
        }
        decl.put("modules", modules);
        decl.put("requires", requiresNode);
        decl.put("files", files);

        Files.createDirectories(out.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out), StandardCharsets.UTF_8)) {
            putZipEntry(zip, "attachments.json", CANONICAL.writeValueAsBytes(decl));
            putZipEntry(zip, "delivery/vars.env", varsBytes);
            putZipEntry(zip, "delivery/deploy-package.zip", dataBytes);
            putZipEntry(zip, "extras/schema.sql", schemaBytes);
        }
    }

    private static void putZipEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    /** 模块集（null=不过滤全量导出）；system 模块恒随包，不依赖调用方勾选 */
    static Set<String> moduleFilterOf(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        Set<String> mf = new java.util.LinkedHashSet<>();
        for (String m : modules) {
            if (!isBlank(m)) {
                mf.add(m.trim());
            }
        }
        return mf.isEmpty() ? null : mf;
    }

    private static String modulesCsv(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        String csv = String.join(",", modules.stream().map(String::trim).filter(s -> !s.isBlank()).toList());
        return csv.isBlank() ? null : csv;
    }

    /** deliver-studio 直推配置状态（签发对话框展示） */
    public StudioInfo studioInfo() {
        return new StudioInfo(studioPush != null && studioPush.enabled(),
                studioPush == null ? "" : studioPush.baseUrl());
    }

    /** 可选业务模块目录（classpath 版本化脚本 biz 去重，剔除基座 system/platform/deliver）——签发对话框模块多选项 */
    public List<String> availableModules() {
        Set<String> out = new java.util.TreeSet<>();
        for (var m : new TenantMigrationScanner().scan("mysql")) {
            String biz = m.biz();
            if (!biz.isBlank() && !"system".equals(biz) && !"platform".equals(biz) && !"deliver".equals(biz)) {
                out.add(biz);
            }
        }
        return new ArrayList<>(out);
    }

    /** 表结构 DDL（extras/schema.sql）：逐表 SHOW CREATE TABLE，改写幂等 CREATE TABLE IF NOT EXISTS
     * （并剥 AUTO_INCREMENT 噪声）。表集合与本次导出的 jsonl 1:1（模块过滤/补丁表同口径）；
     * 全栈模式经 mysql 组件 extras→initdb 挂载首启预置，复用库模式由 deliver-console 按组件 schema 声明
     * 一次性容器执行；应用启动自足化直建仍是结构权威与升级通道。
     */
    private String schemaSql(Connection conn, List<String> tables) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- 表结构 DDL（平台「交付附件签发」导出自动生成，与本次导出的数据表集合一致）\n");
        sql.append("-- 幂等（CREATE TABLE IF NOT EXISTS）：extras 顶层经 MySQL 首启 initdb 挂载预置，\n");
        sql.append("-- 或由 deliver-console 在复用库上执行（组件 schema 声明）；应用启动自足化直建是结构权威与升级通道\n");
        sql.append("SET NAMES utf8mb4;\n");
        for (String table : tables) {
            String safe = table.replace("`", "");
            try (var ps = conn.prepareStatement("SHOW CREATE TABLE `" + safe + "`");
                 var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ddl = rs.getString(2);
                    if (ddl == null) {
                        continue;
                    }
                    ddl = ddl.replaceFirst("(?i)^CREATE\\s+TABLE\\s+", "CREATE TABLE IF NOT EXISTS ");
                    ddl = ddl.replaceFirst("(?i)\\s+AUTO_INCREMENT=\\d+", "");
                    sql.append(ddl).append(";\n");
                }
            }
        }
        return sql.toString();
    }

    /** delivery/vars.env：租户身份 + license 公钥（console 表单自动预填；公钥由平台私钥派生） */
    private String varsEnv(SysTenant tenant) {
        String publicKey = LicenseCodec.publicKeyBase64Of(privateKey);
        return """
                # 交付预置变量（平台「交付附件签发」导出生成；deliver-console 表单自动预填）
                TENANT_ID=%d
                TENANT_CODE=%s
                TENANT_NAME=%s
                LICENSE_PUBLIC_KEY=%s
                """.formatted(tenant.getId(), tenant.getCode(), tenant.getName(),
                isBlank(publicKey) ? "" : publicKey);
    }

    /** 逐表导出：行数预检 → 流式 JSONL（hash 由 PackageWriter 流式记账） */
    private ExportedTable exportTable(PackageWriter writer, Connection conn, String table,
                                      boolean shared, long tenantId) throws SQLException, IOException {
        long count = exporter.countRows(conn, table, shared, tenantId);
        if (maxTableRows > 0 && count > maxTableRows) {
            throw new ServiceException(400, "表 " + table + " 行数 " + count
                    + " 超过单表上限 " + maxTableRows + "（mse.deliver.export.max-table-rows）");
        }
        String entry = "data/" + table + ".jsonl";
        long rows;
        try (PackageWriter.HashingOutputStream out = writer.openStream(entry)) {
            rows = exporter.writeTableJsonl(conn, table, shared, tenantId, out);
        }
        writer.closeEntry();
        PackageWriter.EntryHash hash = writer.entries().get(writer.entries().size() - 1);
        return new ExportedTable(table, entry, rows, hash.size(), hash.sha256());
    }

    /** 升级包补丁表：清洗 + 校验均在可导出清单内 */
    private List<String> patchTables(List<String> exportable, List<String> patchTables) {
        List<String> cleaned = patchTables == null ? List.of() : patchTables.stream()
                .filter(s -> !isBlank(s)).map(String::trim).distinct().sorted().toList();
        if (cleaned.isEmpty()) {
            throw new ServiceException(400, "升级包须至少勾选一张数据补丁表");
        }
        List<String> unknown = cleaned.stream().filter(t -> !exportable.contains(t)).toList();
        if (!unknown.isEmpty()) {
            throw new ServiceException(400, "补丁表不在租户库可导出清单中: " + String.join(", ", unknown));
        }
        return cleaned;
    }

    /** 升级包补丁表提交期同步校验：空清单与不在租户库可导出清单内的表在 submit 即拒绝
     * （此前校验在异步任务体，接口 code=0 落 RUNNING 记录后才异步失败——契约与文档不符；
     * 连接租户库清点一次清单，代价可接受）。 */
    private void validatePatchTablesSync(SysTenant tenant, ExportRequest request, boolean full) {
        if (full) {
            return;
        }
        List<String> cleaned = request.patchTables() == null ? List.of() : request.patchTables().stream()
                .filter(s -> !isBlank(s)).map(String::trim).distinct().sorted().toList();
        if (cleaned.isEmpty()) {
            throw new ServiceException(400, "升级包须至少勾选一张数据补丁表");
        }
        SysTenantDatasource ds = resolveDatasource(tenant);
        try (Connection conn = openTenantConnection(tenant, ds)) {
            boolean shared = isSharedMode(ds);
            Set<String> ignoreTables = TenantDbCloner.ignoreTableNames(
                    tenantPropertiesProvider.getIfAvailable(MseTenantProperties::new));
            List<String> exportable = exporter.exportTables(conn, shared, ignoreTables);
            List<String> unknown = cleaned.stream().filter(t -> !exportable.contains(t)).toList();
            if (!unknown.isEmpty()) {
                throw new ServiceException(400, "补丁表不在租户库可导出清单中: " + String.join(", ", unknown));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(400, "补丁表校验失败（租户库连接异常）：" + e.getMessage());
        }
    }

    /** 内层数据包 manifest（formatVersion:1，RS256 签名原文；导入端 DeliverImportService 消费）：
     *  仅列包内实际存在的数据文件（license + data/*.jsonl）——导入端逐区段校验文件存在性与哈希。 */
    private Map<String, Object> buildManifest(String type, SysTenant tenant, boolean shared,
                                              ExportRequest request, Map<String, String> requires,
                                              List<ExportedTable> exportedTables,
                                              List<PackagedFile> auxFiles) {
        Map<String, Object> tenantNode = new LinkedHashMap<>();
        tenantNode.put("id", tenant.getId());
        tenantNode.put("code", tenant.getCode());
        tenantNode.put("name", tenant.getName());

        Map<String, Object> requiresNode = new LinkedHashMap<>();
        requiresNode.put("platformVersion", platformVersion);
        requiresNode.put("perBizSchemaVersion", new LinkedHashMap<>(requires));

        List<Map<String, Object>> tableNodes = new ArrayList<>();
        for (ExportedTable table : exportedTables) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("table", table.table());
            node.put("rows", table.rows());
            node.put("file", table.file());
            node.put("size", table.size());
            node.put("sha256", table.sha256());
            tableNodes.add(node);
        }
        List<Map<String, Object>> fileNodes = fileNodes(auxFiles);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", 1);
        manifest.put("packageType", type);
        manifest.put("createdAt", System.currentTimeMillis());
        manifest.put("platformVersion", platformVersion);
        manifest.put("tenant", tenantNode);
        manifest.put("tenantId", tenant.getId());
        manifest.put("sharedMode", shared);
        manifest.put("licenseId", isBlank(request.licenseId()) ? "" : request.licenseId());
        manifest.put("requires", requiresNode);
        manifest.put("tables", tableNodes);
        manifest.put("files", fileNodes);
        if (!isBlank(request.remark())) {
            manifest.put("remark", request.remark().trim());
        }
        return manifest;
    }

    private List<Map<String, Object>> fileNodes(List<PackagedFile> files) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (PackagedFile file : files) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("file", file.file());
            node.put("size", file.size());
            node.put("sha256", file.sha256());
            nodes.add(node);
        }
        return nodes;
    }

    /** 表→biz 归属映射（各 biz 版本化脚本的 CREATE TABLE 解析；全部通道都解析——BASE 通道建的基座数据表同样要有归属） */
    private Map<String, String> tableBizByScript(String dbType) {
        Map<String, String> out = new java.util.HashMap<>();
        var createPattern = java.util.regex.Pattern.compile(
                "(?i)\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[\"'`]?([A-Za-z][A-Za-z0-9_$]*)");
        for (var m : new TenantMigrationScanner().scan(dbType)) {
            if (m.sql() == null) {
                continue;
            }
            var matcher = createPattern.matcher(m.sql());
            while (matcher.find()) {
                out.putIfAbsent(matcher.group(1).toLowerCase(Locale.ROOT), m.biz());
            }
        }
        return out;
    }

    // ===== 记录查询/下载 =====

    public PageResult<PackageView> pagePackages(long pageNum, long pageSize, Long tenantId, Integer status) {
        Page<SysDeliverPackage> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysDeliverPackage>()
                        .eq(tenantId != null, SysDeliverPackage::getTenantId, tenantId)
                        .eq(status != null, SysDeliverPackage::getStatus, status)
                        .orderByDesc(SysDeliverPackage::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize,
                page.getRecords().stream().map(this::toView).toList());
    }

    /** 签发记录详情（含内层 manifest 快照，审计用） */
    public SysDeliverPackage getPackageDetail(Long id) {
        SysDeliverPackage record = getById(id);
        if (record == null) {
            throw new ServiceException(404, "签发记录不存在");
        }
        return record;
    }

    public void downloadPackage(Long id, jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response) throws IOException {
        SysDeliverPackage record = getById(id);
        if (record == null) {
            throw new ServiceException(404, "签发记录不存在");
        }
        if (!Integer.valueOf(SysDeliverPackage.STATUS_DONE).equals(record.getStatus())) {
            throw new ServiceException(400, "附件包尚未签发完成（当前进度 " + record.getProgress() + "%）");
        }
        serveFile(request, response, Path.of(record.getFilePath()), record.getFileName(), record.getFileSha256());
    }

    // ===== 内部 =====

    private void progress(SysDeliverPackage record, int pct, String stage) {
        record.setProgress(Math.max(0, Math.min(100, pct)));
        record.setStage(stage);
        try {
            packageMapper.update(null, new LambdaUpdateWrapper<SysDeliverPackage>()
                    .eq(SysDeliverPackage::getId, record.getId())
                    .set(SysDeliverPackage::getProgress, record.getProgress())
                    .set(SysDeliverPackage::getStage, stage));
        } catch (Exception e) {
            log.warn("[mse-deliver] 进度更新失败（不影响签发）: {}", e.getMessage());
        }
    }

    private void markFailed(SysDeliverPackage record, String error) {
        try {
            packageMapper.update(null, new LambdaUpdateWrapper<SysDeliverPackage>()
                    .eq(SysDeliverPackage::getId, record.getId())
                    .set(SysDeliverPackage::getStatus, SysDeliverPackage.STATUS_FAILED)
                    .set(SysDeliverPackage::getErrorMsg, error == null ? "" : error.substring(0, Math.min(1900, error.length())))
                    .set(SysDeliverPackage::getStage, "失败"));
        } catch (Exception e) {
            log.warn("[mse-deliver] 失败状态落库异常: {}", e.getMessage());
        }
    }

    private SysTenantDatasource resolveDatasource(SysTenant tenant) {
        String dsKey = tenant.getDsKey();
        if (isBlank(dsKey)) {
            return null;
        }
        return datasourceMapper.selectOne(new LambdaQueryWrapper<SysTenantDatasource>()
                .eq(SysTenantDatasource::getDsKey, dsKey));
    }

    private boolean isSharedMode(SysTenantDatasource ds) {
        return ds == null || !SysTenantDatasource.MODE_DEDICATED.equals(ds.getDsMode());
    }

    /** 租户库连接：已登记数据源（独立库/共享库）→ DriverManager 直连；未登记（内置主库）→ 主数据源 */
    private Connection openTenantConnection(SysTenant tenant, SysTenantDatasource ds) throws SQLException {
        if (ds == null) {
            DataSource master = masterDataSourceProvider.getIfAvailable();
            if (master == null) {
                throw new ServiceException(500, "主数据源不可用");
            }
            return master.getConnection();
        }
        TenantDatasourceDefinition def = new TenantDatasourceDefinition();
        def.setId(ds.getId());
        def.setDsKey(ds.getDsKey());
        def.setDsMode(ds.getDsMode());
        def.setServiceName(ds.getServiceName());
        def.setDbType(ds.getDbType());
        def.setHost(ds.getHost());
        def.setPort(ds.getPort());
        def.setDbName(ds.getDbName());
        def.setParams(ds.getParams());
        def.setUsername(ds.getUsername());
        def.setPassword(ds.getPassword());
        def.setStatus(ds.getStatus());
        def.setRemark(ds.getRemark());
        int old = DriverManager.getLoginTimeout();
        DriverManager.setLoginTimeout(15);
        try {
            return DriverManager.getConnection(TenantDsUrlBuilder.build(def), ds.getUsername(), decrypt(ds.getPassword()));
        } finally {
            DriverManager.setLoginTimeout(old);
        }
    }

    private String decrypt(String stored) {
        try {
            return AesCipher.decrypt(stored, AesCipher.parseKey(
                    tenantPropertiesProvider.getIfAvailable(MseTenantProperties::new).getDynamicDs().getEncryptKey()));
        } catch (Exception e) {
            throw new ServiceException(400, "租户数据源密码解密失败：请检查 mse.tenant.dynamic-ds.encrypt-key 与保存时是否一致");
        }
    }

    // ===== 签名 URL 下载（浏览器原生下载：免会话直下 + Range 断点续传） =====

    /**
     * 签发附件包下载 URL：浏览器原生下载（a 标签/下载管理器）带不了 Authorization 头，
     * 故权限在签发时校验、下载凭短时签名 token 免会话直下（token 即凭证，同 /fs/asset 通道口径）。
     */
    public DownloadUrlView issuePackageDownloadUrl(Long id) {
        SysDeliverPackage record = getById(id);
        if (record == null) {
            throw new ServiceException(404, "签发记录不存在");
        }
        if (!Integer.valueOf(SysDeliverPackage.STATUS_DONE).equals(record.getStatus())) {
            throw new ServiceException(400, "附件包尚未签发完成（当前进度 " + record.getProgress() + "%）");
        }
        long expireAt = System.currentTimeMillis() + downloadUrlTtlSeconds * 1000;
        String payload = "P" + id + "|" + expireAt;
        return new DownloadUrlView("/tenant/deliver/download/" + b64(payload.getBytes(StandardCharsets.UTF_8))
                + "." + b64(hmac(payload)), expireAt);
    }

    /** 公开下载入口（免会话，网关白名单放行）：验签 + 有效期后按 Range 出附件包 */
    public void publicDownload(String token, jakarta.servlet.http.HttpServletRequest request,
                               jakarta.servlet.http.HttpServletResponse response) throws IOException {
        String subject = verifyDownloadToken(token);
        long id;
        try {
            id = Long.parseLong(subject.substring(1));
        } catch (RuntimeException e) {
            throw new ServiceException(403, "无效的下载凭证");
        }
        if (subject.charAt(0) != 'P') {
            throw new ServiceException(403, "无效的下载凭证");
        }
        SysDeliverPackage record = getById(id);
        if (record == null || !Integer.valueOf(SysDeliverPackage.STATUS_DONE).equals(record.getStatus())) {
            throw new ServiceException(404, "附件包不存在或不可下载");
        }
        serveFile(request, response, Path.of(record.getFilePath()), record.getFileName(), record.getFileSha256());
    }

    /** 校验下载凭证：验签（常量时间比对）+ 有效期，返回 subject（类型字符 + id）；无效一律 403 */
    private String verifyDownloadToken(String token) {
        final String deny = "下载凭证无效或已过期，请回到页面重新下载";
        if (token == null) {
            throw new ServiceException(403, deny);
        }
        try {
            int dot = token.indexOf('.');
            if (dot <= 0 || dot == token.length() - 1) {
                throw new ServiceException(403, deny);
            }
            byte[] payload = Base64.getUrlDecoder().decode(token.substring(0, dot));
            byte[] signature = Base64.getUrlDecoder().decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(hmac(new String(payload, StandardCharsets.UTF_8)), signature)) {
                throw new ServiceException(403, deny);
            }
            String subject = new String(payload, StandardCharsets.UTF_8);
            int bar = subject.lastIndexOf('|');
            if (bar < 1 || Long.parseLong(subject.substring(bar + 1)) < System.currentTimeMillis()) {
                throw new ServiceException(403, deny);
            }
            return subject.substring(0, bar);
        } catch (IllegalArgumentException e) { // 非法 base64 / 非数字过期位
            throw new ServiceException(403, deny);
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(downloadSignKey, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 不可用", e);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * 文件输出核心（Range/If-Range 断点续传）：浏览器下载管理器的暂停/续传/断线重连
     * 靠 206 Partial Content + 强 ETag（If-Range 不匹配则忽略 Range 整包重发，防介质变更后续传出脏字节）。
     * 仅支持单区间（多区间罕见，退化为整包 200）；客户端主动断开（暂停/取消）属常态，不按错误上报。
     */
    private void serveFile(jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response,
                           Path file, String fileName, String etag) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new ServiceException(404, "归档文件不存在（可能已被清理）: " + fileName);
        }
        long total = Files.size(file);
        String strongEtag = etag == null ? null : "\"" + etag + "\"";
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null && strongEtag != null) {
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !strongEtag.equals(ifRange.replaceFirst("^W/", ""))) {
                rangeHeader = null;
            }
        }
        long start = 0;
        long end = total - 1;
        boolean partial = false;
        Matcher matcher = rangeHeader == null ? null : RANGE_HEADER.matcher(rangeHeader.trim());
        if (matcher != null && matcher.matches()) {
            if (!matcher.group(1).isEmpty()) {
                start = Long.parseLong(matcher.group(1));
                if (!matcher.group(2).isEmpty()) {
                    end = Math.min(Long.parseLong(matcher.group(2)), total - 1);
                }
            } else if (!matcher.group(2).isEmpty()) { // 后缀区间 bytes=-N：取最后 N 字节
                start = Math.max(0, total - Long.parseLong(matcher.group(2)));
            }
            if (start >= total || start > end) {
                response.setStatus(416);
                response.setHeader("Content-Range", "bytes */" + total);
                return;
            }
            partial = true;
        }
        response.setStatus(partial ? jakarta.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT
                : jakarta.servlet.http.HttpServletResponse.SC_OK);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Accept-Ranges", "bytes");
        if (strongEtag != null) {
            response.setHeader("ETag", strongEtag);
        }
        long length = end - start + 1;
        response.setContentLengthLong(length);
        if (partial) {
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);
        }
        try (var input = Files.newInputStream(file)) {
            input.skipNBytes(start);
            var out = response.getOutputStream();
            byte[] buffer = new byte[64 * 1024];
            long remaining = length;
            int read;
            while (remaining > 0
                    && (read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                out.write(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException e) {
            log.debug("[mse-deliver] 下载流中断（客户端断开？） {}: {}", fileName, e.getMessage());
        }
    }

    private PackageView toView(SysDeliverPackage record) {
        return new PackageView(record.getId(), record.getTenantId(), record.getTenantCode(),
                record.getPackageType(), record.getLicenseId(), record.getModuleList(),
                record.getStudioDeliveryId(), record.getPushStatus(), record.getSharedMode(),
                record.getStatus(), record.getProgress(), record.getStage(),
                record.getErrorMsg(), record.getTableCount(), record.getRowCount(), record.getFileName(),
                record.getFileSize(), record.getFileSha256(), record.getRemark(), record.getCreateTime());
    }

    private static void closeQuietly(PackageWriter writer) {
        try {
            writer.close();
        } catch (IOException ignored) {
            // 失败路径收尾，原始异常优先
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 失败路径收尾，原始异常优先
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
