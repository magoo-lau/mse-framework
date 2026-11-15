package vip.mseapp.mybatis.datasource;

/**
 * 租户数据源 JDBC 连接串构造（三方言模板与静态配置基线一致）：
 *
 * <ul>
 *   <li>MySQL：独立 database，固定 utf8/东八区等基线参数；{@code nullCatalogMeansCurrent=true}
 *       将 DatabaseMetaData 元数据搜索限定在当前库（Connector/J 8 缺省跨库搜索，同实例上
 *       会"看见"其他库同名表——如主库的 ACT_ 表，导致 Flowable 建表判定误走升级路径）</li>
 *   <li>达梦：单实例多 schema，{@code schema=} 指定（习惯大写）</li>
 *   <li>金仓：独立 database + {@code currentSchema=库,public}</li>
 * </ul>
 */
public final class TenantDsUrlBuilder {

    private TenantDsUrlBuilder() {
    }

    /** 按 db_type 构造 JDBC URL（def.params 非空时以 & 追加） */
    public static String build(TenantDatasourceDefinition def) {
        String host = require(def.getHost(), "host");
        Integer port = def.getPort();
        if (port == null || port <= 0) {
            throw new IllegalArgumentException("port 不能为空");
        }
        String dbName = require(def.getDbName(), "dbName");
        String base = switch (normalizeType(def.getDbType())) {
            case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
                    + "&allowPublicKeyRetrieval=true&useInformationSchema=true&nullCatalogMeansCurrent=true";
            case "dm" -> "jdbc:dm://" + host + ":" + port + "?schema=" + dbName + "&characterEncoding=UTF-8";
            case "kingbase" -> "jdbc:kingbase8://" + host + ":" + port + "/" + dbName
                    + "?currentSchema=" + dbName + ",public";
            default -> throw new IllegalArgumentException("不支持的数据库类型：" + def.getDbType() + "（mysql/dm/kingbase）");
        };
        String params = def.getParams();
        return params == null || params.isBlank() ? base : base + "&" + params.trim();
    }

    /** db_type 对应的 JDBC 驱动类名 */
    public static String driverClass(String dbType) {
        return switch (normalizeType(dbType)) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "dm" -> "dm.jdbc.driver.DmDriver";
            case "kingbase" -> "com.kingbase8.Driver";
            default -> throw new IllegalArgumentException("不支持的数据库类型：" + dbType + "（mysql/dm/kingbase）");
        };
    }

    /**
     * 数据源定义与主库 JDBC URL 是否指向同一数据库（同库绑定降级共享语义的判定依据）。
     * 比对维度：方言族 + 主机（localhost≡127.0.0.1）+ 端口（缺省按方言补齐）+ 库名/Schema（区分大小写）。
     * 主库 URL 无法解析（非常规形态）时恒返回 false，检测自动停用。
     */
    public static boolean sameTarget(TenantDatasourceDefinition def, String mainJdbcUrl) {
        Target main = parse(mainJdbcUrl);
        if (main == null) {
            return false;
        }
        String type;
        String host;
        int port;
        try {
            type = normalizeType(def.getDbType());
            host = normalizeHost(require(def.getHost(), "host"));
        } catch (Exception e) {
            return false;
        }
        if (!main.type().equals(type)) {
            return false;
        }
        if (!main.host().equals(host)) {
            return false;
        }
        port = def.getPort() == null || def.getPort() <= 0 ? defaultPort(type) : def.getPort();
        if (port != main.port()) {
            return false;
        }
        String db = def.getDbName();
        return db != null && !db.isBlank() && db.trim().equals(main.db());
    }

    /** 解析 jdbc:type://host[:port][/db][?params] 形态的 URL（达梦库取 schema 参数）；无法解析返回 null */
    static Target parse(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) {
            return null;
        }
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        String rawType = jdbcUrl.substring(5, schemeEnd).trim().toLowerCase();
        String rest = jdbcUrl.substring(schemeEnd + 3);
        String query = "";
        int q = rest.indexOf('?');
        if (q >= 0) {
            query = rest.substring(q + 1);
            rest = rest.substring(0, q);
        }
        String hostPart = rest;
        String db = "";
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            hostPart = rest.substring(0, slash);
            db = rest.substring(slash + 1);
        }
        String host;
        int port = -1;
        int colon = hostPart.indexOf(':');
        if (colon >= 0) {
            host = hostPart.substring(0, colon);
            try {
                port = Integer.parseInt(hostPart.substring(colon + 1));
            } catch (NumberFormatException e) {
                port = -1;
            }
        } else {
            host = hostPart;
        }
        // 达梦单实例多 schema：库名在 schema 参数而非路径
        if (db.isBlank() && rawType.equals("dm")) {
            db = param(query, "schema");
        }
        String type = rawType.equals("kingbase8") ? "kingbase" : rawType;
        if (port <= 0) {
            port = defaultPort(type);
        }
        return new Target(type, normalizeHost(host), port, db.trim());
    }

    private static String param(String query, String key) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && key.equalsIgnoreCase(pair.substring(0, eq).trim())) {
                return pair.substring(eq + 1).trim();
            }
        }
        return "";
    }

    private static String normalizeHost(String host) {
        String normalized = host.trim().toLowerCase();
        return "localhost".equals(normalized) ? "127.0.0.1" : normalized;
    }

    private static int defaultPort(String type) {
        return switch (type) {
            case "mysql", "mariadb" -> 3306;
            case "dm" -> 5236;
            case "kingbase" -> 54321;
            default -> -1;
        };
    }

    /** 解析后的连接目标（方言族归一：kingbase8→kingbase） */
    record Target(String type, String host, int port, String db) {
    }

    private static String normalizeType(String dbType) {
        return require(dbType, "dbType").trim().toLowerCase();
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }
}
