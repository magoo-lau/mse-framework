package vip.mseapp.mybatis.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限标记：标注在 Mapper 查询方法上，声明该方法按当前用户的数据范围过滤行。
 *
 * <p>生效条件三选一失败即原样执行：无用户上下文（系统内部调用）、数据范围为「全部」、
 * 或部署形态未提供 {@link DataScopeParamResolver}（如 mse-auth 这类无业务库的服务）。
 * 列名可按表覆盖，缺省 dept_id / create_by。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /** 部门列名 */
    String deptColumn() default "dept_id";

    /** 用户列名（仅本人范围时比对） */
    String userColumn() default "create_by";
}
