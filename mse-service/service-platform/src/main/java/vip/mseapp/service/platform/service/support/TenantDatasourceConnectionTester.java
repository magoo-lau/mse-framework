package vip.mseapp.service.platform.service.support;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.service.platform.entity.SysTenantDatasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 租户数据源连接测试（自 SysTenantDatasourceServiceImpl 拆出的内部协作者，业务工程勿依赖）：
 * 表单/存量行解析完成后的目标库直连试连（SELECT 1 计时）。
 */
// 控制面协作者：类级 NOT_SUPPORTED 防错库堤坝，勿删（范式见 SysTenantRouteServiceImpl 类注释）
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TenantDatasourceConnectionTester {

    private final TenantDatasourceSupport support;

    public TenantDatasourceConnectionTester(TenantDatasourceSupport support) {
        this.support = support;
    }

    /**
     * 直连试连并返回耗时毫秒。表单携带的是明文密码（新增页/编辑页未保存的修改）直接试连；
     * storedPassword=true 表示密码取自库中密文需解密（表单/存量行的合并由调用方完成）。
     */
    public long test(SysTenantDatasource form, boolean storedPassword) {
        String url = TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(form));
        String password = storedPassword ? support.decrypt(form.getPassword()) : form.getPassword();
        int old = DriverManager.getLoginTimeout();
        DriverManager.setLoginTimeout(5);
        try (Connection conn = DriverManager.getConnection(url, form.getUsername(), password);
             Statement stmt = conn.createStatement()) {
            long start = System.currentTimeMillis();
            stmt.execute("SELECT 1");
            return System.currentTimeMillis() - start;
        } catch (SQLException e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "连接失败：" + e.getMessage());
        } finally {
            DriverManager.setLoginTimeout(old);
        }
    }
}
