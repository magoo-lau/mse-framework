package vip.mseapp.service.platform.service.support;

import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.datasource.TenantDatasourceDefinition;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.platform.entity.SysTenantDatasource;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * 租户数据源管理的共享小工具（自 SysTenantDatasourceServiceImpl 拆出）：
 * 纯函数静态工具（实体→连接定义、文本判定/截断、三通道结果判定、缺库识别、标识符白名单）
 * + 密码加解密（依赖 mse.tenant.dynamic-ds.encrypt-key，经实例方法持有 provider）。
 * 内部协作者专用，业务工程勿依赖。
 */
public class TenantDatasourceSupport {

    /** SQL 标识符（库名/表名）白名单：拼接 DDL 前防御性校验 */
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$]+$");

    private final ObjectProvider<MseTenantProperties> tenantPropertiesProvider;

    public TenantDatasourceSupport(ObjectProvider<MseTenantProperties> tenantPropertiesProvider) {
        this.tenantPropertiesProvider = tenantPropertiesProvider;
    }

    public String encrypt(String plain) {
        return AesCipher.encrypt(plain, cipherKey());
    }

    public String decrypt(String stored) {
        try {
            return AesCipher.decrypt(stored, cipherKey());
        } catch (IllegalStateException e) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(),
                    "密码解密失败：请检查 mse.tenant.dynamic-ds.encrypt-key 与保存时是否一致");
        }
    }

    private javax.crypto.SecretKey cipherKey() {
        return AesCipher.parseKey(tenantPropertiesProvider.getIfAvailable(MseTenantProperties::new)
                .getDynamicDs().getEncryptKey());
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 落库消息截断（超长省略号收尾） */
    public static String truncate(String message, int max) {
        if (message == null || message.length() <= max) {
            return message;
        }
        return message.substring(0, Math.max(0, max - 1)) + "…";
    }

    /** 三通道首个失败/checksum 冲突描述（null=全部干净；冲突同失败对待——结构不确定不放行） */
    public static String firstProblem(TenantSchemaMigrator.Result... results) {
        for (TenantSchemaMigrator.Result result : results) {
            if (result.failed() > 0) {
                return result.firstError();
            }
            if (result.conflicts() > 0) {
                return "checksum 冲突: " + result.firstConflict();
            }
        }
        return null;
    }

    /** 物理库不存在：SQLState 3D000（PG 系）或消息含 Unknown database（MySQL 1049 报 42000，只能按消息识别） */
    public static boolean isDatabaseAbsent(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException se) {
                String state = se.getSQLState();
                String message = se.getMessage() == null ? "" : se.getMessage();
                if ("3D000".equals(state) || message.contains("Unknown database")) {
                    return true;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /** SQL 标识符白名单校验（拼接 DDL 前防御），非法即抛 BAD_REQUEST */
    public static String identifier(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (!IDENTIFIER.matcher(trimmed).matches()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    field + "非法（仅允许字母/数字/下划线/$）：" + value);
        }
        return trimmed;
    }

    public static TenantDatasourceDefinition toDefinition(SysTenantDatasource ds) {
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
        return def;
    }
}
