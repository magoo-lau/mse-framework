package vip.mseapp.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务表独立库路由标注（历史兼容保留，路由判定已不再读取本注解）。
 *
 * <p>现行路由语义（common-mybatis 统一执行）：
 * <ul>
 *   <li>租户绑定数据源（sys_tenant.ds_key 全局默认 / 服务级隔离配置）后，<b>全部非全局表</b>
 *       （含 sys_user 等身份表）随租户路由到绑定库——dedicated 独立库整库私有（一库一租户，
 *       tenant_id 列保留但行级插件免过滤），shared 共享池多租户同库靠 tenant_id 行级隔离；</li>
 *   <li>绑定库与主库同库：自动降级共享库语义（tenant_id 行级隔离照常生效）；</li>
 *   <li>未绑定数据源的租户：数据留共享主库，tenant_id 行级隔离。</li>
 * </ul>
 *
 * <p>本注解保留仅为存量实体标注兼容，新业务实体无需标注。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TenantDatasource {
}
