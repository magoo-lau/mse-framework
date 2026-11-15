package vip.mseapp.service.system.catalog;

import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.entity.SysPermission;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * 目录变更批次的幂等 SQL 渲染器（纯静态、三方言通用）。
 *
 * <p>渲染约定（与既有脚本幂等范式一致，禁 ON DUPLICATE KEY 等方言特有语法）：
 * <ul>
 *   <li><b>upsert</b> = 业务键外 id 墓碑清障（仅带业务唯一键的表）+
 *       {@code UPDATE 全列 WHERE id=? AND deleted=0} +
 *       {@code INSERT(..) SELECT .. FROM DUAL WHERE NOT EXISTS(..)}——行存在且未删→覆盖；
 *       行已逻辑删（同 id 重放）→UPDATE 落空且 INSERT 被 NOT EXISTS（不看 deleted）挡住→保持删除；
 *       行不存在→INSERT 补齐。三种状态均幂等收敛。带业务唯一键的表（sys_permission.uk_code、
 *       sys_micro_app.uk_micro_app_name）另须处理<b>异 id 软删墓碑占码</b>：uk 只看业务键不看
 *       deleted，改名扇出（UPDATE 改码）或同码重建（INSERT 补齐）都会撞唯一索引令该批次
 *       Duplicate entry 永久失败并以 head-of-line 阻塞整个待扇出窗口——先删他 id 的 deleted=1
 *       占码行（真源语义：该码属于本 id 行，墓碑是陈旧垃圾；同 id 墓碑不受影响，保持删除），
 *       NOT EXISTS 仍按业务键防同码不同 id 的存活行。</li>
 *   <li><b>delete</b> = {@code UPDATE SET deleted=1 WHERE id=?}（镜像 MyBatis-Plus 逻辑删除）。</li>
 *   <li>字符串值单引号 doubling 转义；NULL 渲染为 NULL；时间渲染 'yyyy-MM-dd HH:mm:ss'。</li>
 * </ul>
 */
public final class CatalogChangeSqlRenderer {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CatalogChangeSqlRenderer() {
    }

    /** 菜单行 upsert（id/create_by/create_time/deleted 不进 UPDATE；INSERT 携带全列快照） */
    public static String renderUpsert(SysMenu menu) {
        return upsert(vip.mseapp.service.system.entity.SysCatalogChange.TABLE_MENU,
                new String[]{"id", "parent_id", "name", "type", "menu_scope", "client", "path",
                        "micro_name", "component", "icon", "sort", "visible", "status",
                        "create_by", "create_time", "update_by", "update_time", "deleted"},
                new Object[]{menu.getId(), menu.getParentId(), menu.getName(), menu.getType(),
                        menu.getMenuScope(), menu.getClient(), menu.getPath(), menu.getMicroName(),
                        menu.getComponent(), menu.getIcon(), menu.getSort(), menu.getVisible(),
                        menu.getStatus(), menu.getCreateBy(), menu.getCreateTime(), menu.getUpdateBy(),
                        menu.getUpdateTime(), menu.getDeleted()},
                null);
    }

    /** 权限点行 upsert（uk_code：业务键一并参与防重） */
    public static String renderUpsert(SysPermission permission) {
        return upsert(vip.mseapp.service.system.entity.SysCatalogChange.TABLE_PERMISSION,
                new String[]{"id", "menu_id", "module", "name", "code", "sort", "status", "remark",
                        "create_by", "create_time", "update_by", "update_time", "deleted"},
                new Object[]{permission.getId(), permission.getMenuId(), permission.getModule(),
                        permission.getName(), permission.getCode(), permission.getSort(), permission.getStatus(),
                        permission.getRemark(), permission.getCreateBy(), permission.getCreateTime(),
                        permission.getUpdateBy(), permission.getUpdateTime(), permission.getDeleted()},
                new String[][]{{"code", permission.getCode()}});
    }

    /** 子应用注册行 upsert（uk_micro_app_name：业务键一并参与防重） */
    public static String renderUpsert(SysMicroApp app) {
        return upsert(vip.mseapp.service.system.entity.SysCatalogChange.TABLE_MICRO_APP,
                new String[]{"id", "name", "title", "dev_entry", "prod_entry", "side", "client",
                        "sort", "status", "remark", "create_by", "create_time", "update_by",
                        "update_time", "deleted"},
                new Object[]{app.getId(), app.getName(), app.getTitle(), app.getDevEntry(),
                        app.getProdEntry(), app.getSide(), app.getClient(), app.getSort(),
                        app.getStatus(), app.getRemark(), app.getCreateBy(), app.getCreateTime(),
                        app.getUpdateBy(), app.getUpdateTime(), app.getDeleted()},
                new String[][]{{"name", app.getName()}});
    }

    /** 逻辑删除批次 SQL */
    public static String renderDelete(String table, Long rowId) {
        return "UPDATE " + table + " SET deleted = 1 WHERE id = " + rowId + ";";
    }

    /** 通用 upsert：columns/values 等长按序对应；UPDATE 排除 id/create_by/create_time/deleted；
     * bizKeys 为 [列名, 值] 二元组（表存在业务唯一键时传入，一并参与 INSERT 防重判定） */
    private static String upsert(String table, String[] columns, Object[] values, String[][] bizKeys) {
        StringJoiner set = new StringJoiner(", ");
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            if ("id".equals(column) || "create_by".equals(column)
                    || "create_time".equals(column) || "deleted".equals(column)) {
                continue;
            }
            set.add(column + " = " + literal(values[i], null));
        }
        String insertColumns = String.join(", ", columns);
        StringJoiner insertValues = new StringJoiner(", ");
        for (int i = 0; i < values.length; i++) {
            // 仅时间列缺省 NOW()；其余 NULL 列必须渲染 NULL——否则可空业务列（menu_id/remark 等）
            // 首建副本会被写成当前时间，按 menu_id IS NULL 过滤的通用权限等语义随之失效
            String nullLiteral = "create_time".equals(columns[i]) || "update_time".equals(columns[i])
                    ? "NOW()" : null;
            insertValues.add(literal(values[i], nullLiteral));
        }
        Object id = values[0];
        StringBuilder notExists = new StringBuilder("WHERE NOT EXISTS (SELECT 1 FROM " + table + " WHERE id = " + id);
        if (bizKeys != null) {
            for (String[] key : bizKeys) {
                notExists.append(" OR ").append(key[0]).append(" = ").append(literal(key[1], null));
            }
        }
        notExists.append(")");
        StringBuilder sql = new StringBuilder();
        // 异 id 软删墓碑清障：uk 只看业务键不看 deleted，他 id 墓碑占码会令 UPDATE（改名）/
        // INSERT（同码重建）撞唯一索引且批次永久失败；同 id 墓碑不受影响（保持删除语义不变）
        if (bizKeys != null) {
            for (String[] key : bizKeys) {
                sql.append("DELETE FROM ").append(table).append(" WHERE ").append(key[0])
                        .append(" = ").append(literal(key[1], null))
                        .append(" AND id <> ").append(id).append(" AND deleted = 1;\n");
            }
        }
        sql.append("UPDATE ").append(table).append(" SET ").append(set)
                .append(" WHERE id = ").append(id).append(" AND deleted = 0;\n")
                .append("INSERT INTO ").append(table).append(" (").append(insertColumns).append(")\n")
                .append("SELECT ").append(insertValues).append(" FROM DUAL\n")
                .append(notExists).append(";");
        return sql.toString();
    }

    /** 值渲染：null 且给了 nullLiteral（如时间缺省 NOW()）用之，否则 NULL */
    private static String literal(Object value, String nullLiteral) {
        if (value == null) {
            return nullLiteral != null ? nullLiteral : "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof LocalDateTime time) {
            return "'" + TIMESTAMP.format(time) + "'";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
