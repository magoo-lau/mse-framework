package vip.mseapp.mybatis.datascope;

/**
 * 数据范围解析 SPI：由持有角色/部门数据的模块实现（service-system），
 * common-mybatis 只依赖接口不依赖实现——与 TenantRouteResolver 同一套装配模式。
 */
public interface DataScopeParamResolver {

    /**
     * 解析当前用户的数据范围。
     *
     * @return null 表示不适用（无用户上下文 / 平台超管等），SQL 原样执行
     */
    DataScopeParam resolve();
}
