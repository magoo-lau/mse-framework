package vip.mseapp.mybatis.datascope;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据范围拦截器：对标注 {@link DataPermission @DataPermission} 的 Mapper 查询，
 * 按当前用户的数据范围（部门/本人）向 WHERE 追加过滤条件。
 *
 * <p>注册顺序必须在租户插件之后（先拼租户条件再拼数据范围，二者可叠加）、分页插件之前
 * （保证 COUNT 语句同样带上数据范围条件）。
 */
public class DataScopeInterceptor implements InnerInterceptor {

    /** 值均为服务端内部数据（列名来自注解、编号来自 Long），无注入面 */
    private static final String DENY_ALL = "1 = 0";

    private final DataScopeParamResolver resolver;
    private final Map<String, Optional<DataPermission>> annotationCache = new ConcurrentHashMap<>();

    public DataScopeInterceptor(DataScopeParamResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            return;
        }
        DataPermission annotation = findAnnotation(ms.getId());
        if (annotation == null) {
            return;
        }
        DataScopeParam scope = resolver.resolve();
        // 无用户上下文（系统内部调用/定时任务）或范围为全部：不过滤
        if (scope == null || scope.type() == DataScopeParam.Type.ALL) {
            return;
        }
        Expression condition = buildCondition(annotation, scope);
        if (condition == null) {
            return;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(boundSql.getSql());
            if (!(statement instanceof Select select)) {
                return;
            }
            PlainSelect plain = select.getPlainSelect();
            Expression where = plain.getWhere();
            if (where == null) {
                plain.setWhere(condition);
            } else {
                // jsqlparser 5.x：Parenthesis 空构造后 setExpression 会越界（expressions.set(0,...)），
                // 用实现了 Expression 的 ParenthesedExpressionList 包裹出括号
                ParenthesedExpressionList<Expression> wrapped = new ParenthesedExpressionList<>(where);
                plain.setWhere(new AndExpression(wrapped, condition));
            }
            PluginUtils.mpBoundSql(boundSql).sql(statement.toString());
        } catch (JSQLParserException e) {
            throw new SQLException("数据权限 SQL 改写失败: " + ms.getId(), e);
        }
    }

    private DataPermission findAnnotation(String mappedStatementId) {
        return annotationCache.computeIfAbsent(mappedStatementId, id -> {
            int dot = id.lastIndexOf('.');
            try {
                Class<?> mapperInterface = Class.forName(id.substring(0, dot));
                String methodName = id.substring(dot + 1);
                for (Method method : mapperInterface.getMethods()) {
                    if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                        return Optional.of(method.getAnnotation(DataPermission.class));
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // 非 mapper 语句（理论上不会进入本拦截器）
            }
            return Optional.empty();
        }).orElse(null);
    }

    private Expression buildCondition(DataPermission annotation, DataScopeParam scope) {
        try {
            return switch (scope.type()) {
                case SELF -> CCJSqlParserUtil.parseCondExpression(
                        annotation.userColumn() + " = " + scope.userId());
                case DEPT -> scope.deptId() == null
                        ? CCJSqlParserUtil.parseCondExpression(DENY_ALL)
                        : CCJSqlParserUtil.parseCondExpression(annotation.deptColumn() + " = " + scope.deptId());
                case DEPT_AND_CHILD, CUSTOM -> {
                    // 空集合 = 无可见数据（如自定义范围未勾选任何部门）
                    if (scope.deptIds() == null || scope.deptIds().isEmpty()) {
                        yield CCJSqlParserUtil.parseCondExpression(DENY_ALL);
                    }
                    String ids = scope.deptIds().stream().map(String::valueOf).collect(Collectors.joining(", "));
                    yield CCJSqlParserUtil.parseCondExpression(annotation.deptColumn() + " IN (" + ids + ")");
                }
                default -> null;
            };
        } catch (JSQLParserException e) {
            // 列名来自注解常量、值来自 Long，正常不可能解析失败
            throw new IllegalStateException("数据权限条件构造失败", e);
        }
    }
}
