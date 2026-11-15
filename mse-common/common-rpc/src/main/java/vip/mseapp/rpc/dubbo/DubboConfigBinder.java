package vip.mseapp.rpc.dubbo;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.annotation.RpcService;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.annotation.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dubbo 配置全量映射器：把 {@code @RpcService.Dubbo} / {@code @RpcReference.Dubbo}
 * （及方法级 Dubbo 原生 {@code @Method}/{@code @Argument} 注解）的全部属性落到原生
 * {@code ServiceConfig}/{@code ReferenceConfig}/{@code MethodConfig}/{@code ArgumentConfig}。
 *
 * <p>未设置约定：String 空串 / 数组空 / int -1 跳过不落（沿用 Dubbo 缺省）；boolean 显式落
 * Config（与原生 @DubboService/@DubboReference 处理一致）。setter 按参数类型择优匹配
 * （Dubbo Config 存在 String/Boolean 等重载，如 stub/accesslog；filter/listener 为逗号串约定，
 * String[] 自动 join）。
 */
public final class DubboConfigBinder {

    private static final Logger log = LoggerFactory.getLogger(DubboConfigBinder.class);

    /** 不走通用逐属性反射的特殊属性（专路径处理） */
    private static final Set<String> SERVICE_SPECIAL = Set.of("methods", "parameters");
    private static final Set<String> METHOD_SPECIAL = Set.of("name", "arguments", "parameters",
            "oninvoke", "onreturn", "onthrow");

    private DubboConfigBinder() {
    }

    /** 服务导出侧：@RpcService.Dubbo 全量属性（含 methods 方法级）落到 ServiceConfig（null 时零动作） */
    public static void bindService(ServiceConfig<?> config, RpcService.Dubbo dubbo) {
        if (dubbo == null) {
            return;
        }
        applyAttributes(config, dubbo, SERVICE_SPECIAL);
        applyParameters(config, dubbo.parameters());
        applyMethods(config, dubbo.methods());
    }

    /** 服务引用侧：@RpcReference.Dubbo 全量属性（含 methods 方法级）落到 ReferenceConfig */
    public static void bindReference(ReferenceConfig<?> config, RpcReference.Dubbo dubbo) {
        if (dubbo == null) {
            return;
        }
        applyAttributes(config, dubbo, SERVICE_SPECIAL);
        applyParameters(config, dubbo.parameters());
        applyMethods(config, dubbo.methods());
    }

    /** Dubbo 原生 @Method[] → List&lt;MethodConfig&gt; 并落到接口级 Config 的 methods 属性 */
    private static void applyMethods(Object config, org.apache.dubbo.config.annotation.Method[] methods) {
        if (methods.length == 0) {
            return;
        }
        apply(config, "methods", toMethodConfigs(methods));
    }

    /** 逐属性反射：跳过排除项与未设置哨兵，按名称+类型择优 setter 落值 */
    private static void applyAttributes(Object config, Annotation source, Set<String> exclude) {
        for (Method attribute : source.annotationType().getDeclaredMethods()) {
            String name = attribute.getName();
            if (exclude.contains(name)) {
                continue;
            }
            Object value;
            try {
                value = attribute.invoke(source);
            } catch (ReflectiveOperationException ex) {
                continue;
            }
            if (isUnset(value)) {
                continue;
            }
            apply(config, name, value);
        }
    }

    /** parameters 专路径：{"key=value"} → Map */
    private static void applyParameters(Object config, String[] parameters) {
        if (parameters.length == 0) {
            return;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String entry : parameters) {
            int idx = entry.indexOf('=');
            if (idx > 0) {
                map.put(entry.substring(0, idx), entry.substring(idx + 1));
            }
        }
        if (!map.isEmpty()) {
            apply(config, "parameters", map);
        }
    }

    /** Dubbo 原生 @Method[] → List&lt;MethodConfig&gt;（含回调三件套拆分与 @Argument 参数级配置；全限定名避免与 java.lang.reflect.Method 混淆） */
    private static List<MethodConfig> toMethodConfigs(org.apache.dubbo.config.annotation.Method[] methods) {
        List<MethodConfig> result = new ArrayList<>();
        for (org.apache.dubbo.config.annotation.Method method : methods) {
            MethodConfig config = new MethodConfig();
            config.setName(method.name());
            applyAttributes(config, method, METHOD_SPECIAL);
            applyParameters(config, method.parameters());
            applyCallback(config, "oninvoke", method.oninvoke());
            applyCallback(config, "onreturn", method.onreturn());
            applyCallback(config, "onthrow", method.onthrow());
            if (method.arguments().length > 0) {
                List<ArgumentConfig> arguments = new ArrayList<>();
                for (Argument argument : method.arguments()) {
                    ArgumentConfig argumentConfig = new ArgumentConfig();
                    apply(argumentConfig, "index", argument.index());
                    apply(argumentConfig, "type", argument.type());
                    apply(argumentConfig, "callback", argument.callback());
                    arguments.add(argumentConfig);
                }
                config.setArguments(arguments);
            }
            result.add(config);
        }
        return result;
    }

    /** 回调三件套："beanName.methodName" 拆分为实例段（setOnXxx(Object)）+ 方法段（setOnXxxMethod(String)） */
    private static void applyCallback(MethodConfig config, String name, String value) {
        if (value.isEmpty()) {
            return;
        }
        int dot = value.lastIndexOf('.');
        if (dot > 0 && dot < value.length() - 1) {
            apply(config, name, value.substring(0, dot));
            apply(config, name + "Method", value.substring(dot + 1));
        } else {
            apply(config, name + "Method", value);
        }
    }

    /** 哨兵判定：String 空串 / String[] 空 / int -1 视为未设置（boolean 不设哨兵，显式落 Config） */
    private static boolean isUnset(Object value) {
        if (value instanceof String s) {
            return s.isEmpty();
        }
        if (value instanceof String[] arr) {
            return arr.length == 0;
        }
        if (value instanceof Integer i) {
            return i == -1;
        }
        return false;
    }

    /** 名称+参数类型择优调用 setter；无匹配或全部转换失败时跳过并 debug 记录 */
    private static void apply(Object config, String name, Object value) {
        for (String setterName : setterNames(name)) {
            for (Method setter : findSetters(config.getClass(), setterName)) {
                Object converted = convert(value, setter.getParameterTypes()[0]);
                if (converted == null) {
                    continue;
                }
                try {
                    setter.invoke(config, converted);
                    return;
                } catch (ReflectiveOperationException ignored) {
                    // 尝试下一个重载
                }
            }
        }
        log.debug("[mse-rpc] Dubbo 属性 {} 无法映射到 {} 的 setter，跳过", name, config.getClass().getSimpleName());
    }

    /** boolean 的 isXxx 属性，Dubbo Config 侧 setter 为 setXxx（如 isReturn → setReturn） */
    private static List<String> setterNames(String name) {
        List<String> names = new ArrayList<>();
        names.add("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        if (name.startsWith("is") && name.length() > 2) {
            names.add("set" + name.substring(2));
        }
        return names;
    }

    /** 沿继承链收集同名 public 单参 setter（Dubbo Config 的重载分布在不同层级） */
    private static List<Method> findSetters(Class<?> type, String setterName) {
        List<Method> result = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1
                        && Modifier.isPublic(method.getModifiers())) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    /** 镜像注解属性值 → setter 参数类型转换；返回 null 表示该重载不可用 */
    private static Object convert(Object value, Class<?> param) {
        if (param.isInstance(value)) {
            return value;
        }
        if (param == String.class) {
            // filter/listener 等在 Config 侧为逗号串约定；payload 等允许数字字符串
            if (value instanceof String[] arr) {
                return String.join(",", arr);
            }
            if (value instanceof Integer || value instanceof Boolean) {
                return String.valueOf(value);
            }
            return null;
        }
        if (param == int.class || param == Integer.class) {
            if (value instanceof Integer i) {
                return i;
            }
            if (value instanceof String s && s.matches("-?\\d+")) {
                return Integer.valueOf(s);
            }
            return null;
        }
        if (param == boolean.class || param == Boolean.class) {
            return value instanceof Boolean b ? b : null;
        }
        if (Collection.class.isAssignableFrom(param) && value instanceof String[] arr) {
            return Arrays.asList(arr);
        }
        return null;
    }
}
