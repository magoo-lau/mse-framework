package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.DataScopeDTO;

/**
 * 数据范围查询契约（service-system 实现，只读）。
 *
 * <p>与 common-mybatis 的 {@code DataScopeParamResolver} 同一套合并语义（角色 data_scope
 * 取最宽、超管不过滤），但按 userId 显式解析而非请求线程上下文——供不依赖 service-system
 * 的业务微服务（如搜索引擎服务的个人数据过滤）经 {@code @RpcReference} 消费，
 * 单体形态下消费方直接复用本地 Resolver，不经本契约。
 *
 * <p>服务端在调用方租户上下文内执行（Dubbo attachment 透传租户），经租户插件隔离。
 */
public interface RemoteDataScopeService {

    /**
     * 解析指定用户的数据范围。
     *
     * @param userId 用户编号（调用方租户内的用户）
     * @return 数据范围；null 表示不适用（用户不存在 / 超管 / 任一角色为「全部」），调用方不过滤
     */
    DataScopeDTO resolve(Long userId);
}
