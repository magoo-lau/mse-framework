package vip.mseapp.starter.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 部署包启动自动导入（standalone 一键部署主路径）：Go 安装器把最终部署包拷贝到
 * {@code mse.deliver.auto-import-file}（默认 ./conf/deploy-package.zip），应用启动结构自足化
 * 完成后自动 验签 → 计划 → 应用（license 落盘 + 数据装载），成功后包文件改名
 * {@code .imported-<时间戳>} 防重复导入。
 *
 * <p>安全性：RS256 验签（与 license 同一公钥）是唯一门禁——仅平台签名的包可应用，
 * 与 Web 导入端点的登录鉴权互不依赖。失败不阻断应用启动（结构自足、可登录排查），
 * 包文件保持原名，下次启动自动重试；导入幂等（按表整表替换）。
 */
public class DeliverAutoImportRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DeliverAutoImportRunner.class);

    private final DeliverImportService importService;
    private final String autoImportFile;
    private final boolean enabled;

    public DeliverAutoImportRunner(DeliverImportService importService,
                                   @Value("${mse.deliver.auto-import:true}") boolean enabled,
                                   @Value("${mse.deliver.auto-import-file:./conf/deploy-package.zip}") String autoImportFile) {
        this.importService = importService;
        this.enabled = enabled;
        this.autoImportFile = autoImportFile == null || autoImportFile.isBlank()
                ? "./conf/deploy-package.zip" : autoImportFile;
    }

    @Override
    public int getOrder() {
        // 结构自足化（StandaloneBootstrap / MasterSchemaAutoMigration）之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        Path packageFile = Path.of(autoImportFile);
        if (!Files.isRegularFile(packageFile)) {
            return;
        }
        log.info("[mse-deliver] 检测到部署包 {}，等待结构就绪后自动导入……", packageFile);
        if (!awaitSchemaReady(packageFile)) {
            return;
        }
        DeliverImportService.ImportPlan plan = importService.plan(packageFile);
        if (!plan.ready()) {
            log.error("[mse-deliver] 部署包自动导入前置校验未通过（包文件保留，下次启动重试）：{}",
                    String.join("；", plan.checks()));
            return;
        }
        String fileName = packageFile.getFileName().toString();
        DeliverImportService.ApplyResult result = importService.apply(packageFile, fileName);
        if (result.success()) {
            Path imported = packageFile.resolveSibling(fileName + ".imported-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            Files.move(packageFile, imported);
            log.info("[mse-deliver] 部署包自动导入完成：{} 行数据已装载，license 状态 {}，包文件改名 {}。"
                    + "请使用平台交付的管理员账号登录。", result.appliedRows(), result.licenseState(), imported.getFileName());
        } else {
            log.error("[mse-deliver] 部署包自动导入中断（{}）；包文件保留，可修复后重启重试。原因：{}",
                    result.summary(), result.error());
        }
    }

    /** 等待结构自足化完成（以导入审计表/菜单表建出为标志），最长 10 分钟；导入表建出即说明本模块结构就绪 */
    private boolean awaitSchemaReady(Path packageFile) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            try (Connection conn = importService.dataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()"
                                + " AND table_name = 'deliver_import_record'")) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // 结构未就绪/连接未就绪：继续等待
            }
            Thread.sleep(2000);
        }
        log.error("[mse-deliver] 等待结构就绪超时（10 分钟），放弃自动导入：{}", packageFile);
        return false;
    }
}
