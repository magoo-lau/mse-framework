package vip.mseapp.mybatis.datascope;

import java.util.Set;

/**
 * 当前用户的数据范围参数（由业务侧解析后交给 {@link DataScopeInterceptor} 改写 SQL）。
 *
 * @param type    范围类型
 * @param userId  当前用户编号（SELF 时使用）
 * @param deptId  用户所属部门（DEPT 时使用）
 * @param deptIds 展开后的可见部门集合（DEPT_AND_CHILD / CUSTOM 时使用；空集合=无可见数据）
 */
public record DataScopeParam(Type type, Long userId, Long deptId, Set<Long> deptIds) {

    public enum Type {
        /** 全部数据（不过滤） */
        ALL,
        /** 自定义部门集合 */
        CUSTOM,
        /** 本部门 */
        DEPT,
        /** 本部门及以下 */
        DEPT_AND_CHILD,
        /** 仅本人创建 */
        SELF
    }
}
