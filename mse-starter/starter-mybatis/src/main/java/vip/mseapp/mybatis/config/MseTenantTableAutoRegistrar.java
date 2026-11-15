package vip.mseapp.mybatis.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import vip.mseapp.mybatis.data.GlobalBaseEntity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 全局表白名单自动注册：实体继承 {@link GlobalBaseEntity} 的 Mapper 初始化时，
 * 自动把对应表加入租户插件白名单——「基类即声明」，业务模块零白名单配置。
 *
 * <p>设计动机：MyBatis-Plus 行级租户插件按表名白名单豁免，此前业务模块要么在 yaml
 * 全量重抄基座默认白名单（Spring 对 Set 属性整体替换，重抄必然漂移），要么写编程式
 * Registrar 逐表登记。现在声明语义收敛到实体基类：无租户列的表继承 GlobalBaseEntity，
 * 有 tenant_id 列的表继承 BaseEntity——Mapper 装配即自动豁免/自动隔离。
 *
 * <p>时序安全：本处理器拦截 MapperFactoryBean 初始化（表信息在此刻已完成解析），
 * 先于任何可能执行 SQL 的业务 Bean 就绪，启动期查询（如向量库构建读模型表）同样被覆盖。
 * 基座默认白名单（undo_log 等无实体表）仍由 {@link MseTenantProperties} 默认值兜底。
 *
 * <p>注册到 Set 是幂等追加、不整体替换，与 yaml/其他 Registrar 并存无害；
 * 开关关闭（mse.tenant.enabled=false）时为空操作。
 */
public class MseTenantTableAutoRegistrar implements BeanPostProcessor {

    private final MseTenantProperties properties;

    public MseTenantTableAutoRegistrar(MseTenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (properties.isEnabled() && bean instanceof MapperFactoryBean<?> mapperBean) {
            Class<?> entityType = resolveEntityType(mapperBean.getMapperInterface());
            if (entityType != null && GlobalBaseEntity.class.isAssignableFrom(entityType)) {
                properties.getIgnoreTables().add(tableName(entityType));
            }
        }
        return bean;
    }

    /** 表名：优先 @TableName value，缺省按 MyBatis-Plus 默认驼峰转下划线；归一化与 MseTenantLineHandler 比对规则一致 */
    private String tableName(Class<?> entityType) {
        TableName tableName = entityType.getAnnotation(TableName.class);
        String name = tableName != null && tableName.value() != null && !tableName.value().isBlank()
                ? tableName.value()
                : StringUtils.camelToUnderline(entityType.getSimpleName());
        return name.toLowerCase(Locale.ROOT).replace("`", "").replace("\"", "").trim();
    }

    /** 解析 BaseMapper&lt;T&gt; 的实体类型：沿接口继承链向上找参数化为 BaseMapper 的直接声明 */
    private Class<?> resolveEntityType(Class<?> mapperInterface) {
        if (mapperInterface == null) {
            return null;
        }
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(mapperInterface);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            for (Type type : current.getGenericInterfaces()) {
                if (type instanceof ParameterizedType parameterized
                        && parameterized.getRawType() instanceof Class<?> raw
                        && BaseMapper.class.isAssignableFrom(raw)) {
                    Type arg = parameterized.getActualTypeArguments()[0];
                    if (arg instanceof Class<?> entityType) {
                        return entityType;
                    }
                }
                if (type instanceof Class<?> parent) {
                    queue.add(parent);
                } else if (type instanceof ParameterizedType parameterized
                        && parameterized.getRawType() instanceof Class<?> parent) {
                    queue.add(parent);
                }
            }
        }
        return null;
    }
}
