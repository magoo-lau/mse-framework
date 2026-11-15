package vip.mseapp.service.system.service.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vip.mseapp.core.exception.ServiceException;

import java.util.Set;

/**
 * 门户首页 layout_json 结构校验（模板与用户个性化共用的轻量结构约束）。
 *
 * <p>模板：{"layout":[卡片...],"catalog":[卡片...],"themes":[主题...],"defaultThemeId":"...","defaultDarkThemeId":"..."}
 * 用户：{"layout":[卡片...],"themeId":"...","darkThemeId":"...","overrides":{卡片id:{...参数覆盖...}}}。
 * 卡片字段语义由前端渲染器解释，后端只做结构兜底。
 */
public final class HomeLayoutJsonValidator {

    /** 前端内置渲染器类型（mse-ui/src/home/widgets）：未知类型前端按占位渲染，后端直接拒绝 */
    public static final Set<String> WIDGET_TYPES = Set.of("welcome", "links", "list", "stat", "iframe", "micro", "search");
    private static final Set<String> BG_TYPES = Set.of("color", "gradient", "image");
    /** 主题可选多维配色字段（对应前端 HomeTheme.card/navbar/sidebar） */
    private static final Set<String> THEME_COLOR_FIELDS = Set.of("card", "navbar", "sidebar");
    /** 用户参数覆盖允许的字段白名单（仅数据展示类；类型/宽度/启停/可见性不在其列） */
    private static final Set<String> OVERRIDE_KEYS =
            Set.of("title", "api", "method", "params", "fieldMap", "link", "metrics", "refreshSeconds", "placeholder");
    private static final int MAX_OVERRIDES = 50;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private HomeLayoutJsonValidator() {
    }

    /** 校验模板 JSON（layout 必填数组；catalog/themes 可选） */
    public static void validateTemplate(String layoutJson) {
        JsonNode root = parse(layoutJson);
        JsonNode layout = root.path("layout");
        if (!layout.isArray()) {
            throw new ServiceException(400, "layout 须为卡片数组");
        }
        layout.forEach(HomeLayoutJsonValidator::validateWidget);
        JsonNode catalog = root.path("catalog");
        if (!catalog.isMissingNode() && !catalog.isArray()) {
            throw new ServiceException(400, "catalog 须为卡片数组");
        }
        catalog.forEach(HomeLayoutJsonValidator::validateWidget);
        JsonNode themes = root.path("themes");
        if (!themes.isMissingNode()) {
            if (!themes.isArray()) {
                throw new ServiceException(400, "themes 须为主题数组");
            }
            themes.forEach(HomeLayoutJsonValidator::validateTheme);
        }
    }

    /** 校验用户个性化 JSON（layout 必填数组；themeId/overrides 可选） */
    public static void validateUserSetting(String layoutJson) {
        JsonNode root = parse(layoutJson);
        JsonNode layout = root.path("layout");
        if (!layout.isArray()) {
            throw new ServiceException(400, "layout 须为卡片数组");
        }
        layout.forEach(HomeLayoutJsonValidator::validateWidget);
        validateOverrides(root.path("overrides"));
    }

    /** 用户级卡片参数覆盖：{卡片id:{白名单字段...}}，仅数据展示类字段（防御未知键与超量） */
    private static void validateOverrides(JsonNode overrides) {
        if (overrides.isMissingNode() || overrides.isNull()) {
            return;
        }
        if (!overrides.isObject()) {
            throw new ServiceException(400, "overrides 须为「卡片id → 参数覆盖」对象");
        }
        if (overrides.size() > MAX_OVERRIDES) {
            throw new ServiceException(400, "参数覆盖卡片数过多（上限 " + MAX_OVERRIDES + "）");
        }
        overrides.properties().forEach(entry -> {
            JsonNode patch = entry.getValue();
            if (!patch.isObject()) {
                throw new ServiceException(400, "卡片 " + entry.getKey() + " 的参数覆盖须为对象");
            }
            patch.properties().forEach(field -> {
                if (!OVERRIDE_KEYS.contains(field.getKey())) {
                    throw new ServiceException(400, "参数覆盖不支持字段：" + field.getKey());
                }
                validateOverrideField(entry.getKey(), field.getKey(), field.getValue());
            });
        });
    }

    private static void validateOverrideField(String widgetId, String key, JsonNode value) {
        switch (key) {
            case "title" -> {
                if (!value.isString() || value.asText().isBlank() || value.asText().length() > 60) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 title 须为 1-60 字字符串");
                }
            }
            case "api", "link", "placeholder" -> {
                if (!value.isString() || value.asText().length() > 300) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 " + key + " 须为不超过 300 字的字符串");
                }
                if ("api".equals(key) && !value.asText().startsWith("/")) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 api 须为站内接口路径（/ 开头）");
                }
            }
            case "method" -> {
                String method = value.asText("");
                if (!"get".equals(method) && !"post".equals(method)) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 method 须为 get/post");
                }
            }
            case "params" -> {
                if (!value.isObject()) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 params 须为对象");
                }
            }
            case "fieldMap" -> {
                if (!value.isObject()) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 fieldMap 须为对象");
                }
                value.properties().forEach(f -> {
                    if (!f.getValue().isString()) {
                        throw new ServiceException(400, "卡片 " + widgetId + " 的 fieldMap." + f.getKey() + " 须为字符串字段名");
                    }
                });
            }
            case "metrics" -> {
                if (!value.isArray()) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 metrics 须为数组");
                }
                value.forEach(metric -> {
                    if (!metric.isObject() || metric.path("field").asText("").isBlank()) {
                        throw new ServiceException(400, "卡片 " + widgetId + " 的 metrics 项须含 field 字段名");
                    }
                });
            }
            case "refreshSeconds" -> {
                int seconds = value.asInt(-1);
                if (seconds < 0 || seconds > 86400) {
                    throw new ServiceException(400, "卡片 " + widgetId + " 的 refreshSeconds 须在 0-86400 之间");
                }
            }
            default -> throw new ServiceException(400, "参数覆盖不支持字段：" + key);
        }
    }

    private static JsonNode parse(String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            throw new ServiceException(400, "布局 JSON 不能为空");
        }
        try {
            JsonNode node = MAPPER.readTree(layoutJson);
            if (!node.isObject()) {
                throw new ServiceException(400, "布局 JSON 须为对象");
            }
            return node;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(400, "布局 JSON 非法：" + e.getMessage());
        }
    }

    private static void validateWidget(JsonNode item) {
        String type = item.path("type").asText("");
        if (!WIDGET_TYPES.contains(type)) {
            throw new ServiceException(400, "未知卡片类型：" + type);
        }
        if (item.path("title").asText("").isBlank()) {
            throw new ServiceException(400, "卡片标题不能为空");
        }
        int span = item.path("span").asInt(24);
        if (span < 1 || span > 24) {
            throw new ServiceException(400, "卡片宽度 span 须在 1-24 之间（24 栅格）");
        }
    }

    private static void validateTheme(JsonNode item) {
        if (item.path("id").asText("").isBlank() || item.path("name").asText("").isBlank()) {
            throw new ServiceException(400, "主题的 id 与 name 均不能为空");
        }
        String bgType = item.path("bg").path("type").asText("");
        if (!BG_TYPES.contains(bgType)) {
            throw new ServiceException(400, "主题背景类型须为 color/gradient/image");
        }
        if (item.path("bg").path("value").asText("").isBlank()) {
            throw new ServiceException(400, "主题背景值不能为空");
        }
        // 多维配色（可选，卡片/顶栏/侧边栏）：存在时须为扁平字符串对象，键值语义由前端 themeVars 解释
        for (String field : THEME_COLOR_FIELDS) {
            JsonNode node = item.path(field);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (!node.isObject()) {
                throw new ServiceException(400, "主题 " + field + " 须为配色对象");
            }
            node.properties().forEach(entry -> {
                if (!entry.getValue().isString()) {
                    throw new ServiceException(400, "主题 " + field + "." + entry.getKey() + " 须为字符串色值");
                }
            });
        }
    }
}
