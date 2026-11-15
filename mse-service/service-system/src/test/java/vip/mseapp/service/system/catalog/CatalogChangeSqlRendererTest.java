package vip.mseapp.service.system.catalog;

import org.junit.jupiter.api.Test;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.entity.SysPermission;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 目录变更批次 SQL 渲染器测试：形态（UPDATE+INSERT..NOT EXISTS 两段）、幂等（同实体两次渲染
 * 完全一致）、三态收敛语义（UPDATE 带 deleted=0 过滤、INSERT 的 NOT EXISTS 不看 deleted——
 * 已逻辑删行保持删除）、字符串转义与 NULL/时间字面量。
 */
class CatalogChangeSqlRendererTest {

    private SysMenu menu() {
        SysMenu menu = new SysMenu();
        menu.setId(195L);
        menu.setParentId(1L);
        menu.setName("任务调度");
        menu.setType(SysMenu.TYPE_MENU);
        menu.setMenuScope(SysMenu.SCOPE_TENANT);
        menu.setClient(SysMenu.CLIENT_PC);
        menu.setPath("schedule");
        menu.setMicroName(null);
        menu.setComponent("system/Schedule");
        menu.setIcon("Timer");
        menu.setSort(12);
        menu.setVisible(0);
        menu.setStatus(0);
        menu.setCreateBy(1L);
        menu.setCreateTime(LocalDateTime.of(2026, 9, 30, 14, 0, 0));
        menu.setUpdateBy(1L);
        menu.setUpdateTime(LocalDateTime.of(2026, 9, 30, 14, 5, 0));
        menu.setDeleted(0);
        return menu;
    }

    @Test
    void menuUpsertShapeAndIdempotency() {
        String sql = CatalogChangeSqlRenderer.renderUpsert(menu());
        // 两段式：UPDATE（排除 id/create_by/create_time/deleted）+ INSERT（全列 + NOT EXISTS）
        assertTrue(sql.startsWith("UPDATE sys_menu SET parent_id = 1, "));
        // 无业务唯一键的表不产生墓碑清障语句
        assertFalse(sql.contains("DELETE FROM"));
        assertFalse(sql.contains("create_by ="));
        assertFalse(sql.contains("create_time ="));
        assertTrue(sql.contains("WHERE id = 195 AND deleted = 0;"));
        assertTrue(sql.contains("INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, client, path, "
                + "micro_name, component, icon, sort, visible, status, create_by, create_time, "
                + "update_by, update_time, deleted)"));
        assertTrue(sql.contains("WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 195);"));
        // 值字面量：NULL / 'yyyy-MM-dd HH:mm:ss' / 字符串
        assertTrue(sql.contains("micro_name = NULL"));
        assertTrue(sql.contains("'2026-09-30 14:05:00'"));
        // 幂等：同快照两次渲染逐字节一致（账本 checksum 稳定的前提）
        assertEquals(sql, CatalogChangeSqlRenderer.renderUpsert(menu()));
    }

    @Test
    void permissionUpsertEscapesQuotes() {
        SysPermission permission = new SysPermission();
        permission.setId(1911L);
        permission.setMenuId(195L);
        permission.setModule("system");
        permission.setName("任务'查询");
        permission.setCode("schedule:task:list");
        permission.setSort(1);
        permission.setStatus(0);
        permission.setRemark(null);
        String sql = CatalogChangeSqlRenderer.renderUpsert(permission);
        // 单引号 doubling 转义（三方言通用）
        assertTrue(sql.contains("'任务''查询'"));
        assertTrue(sql.contains("remark = NULL"));
        // 带业务键的 upsert：DELETE 清障在前，UPDATE 段随后
        assertTrue(sql.contains("UPDATE sys_permission SET menu_id = 195, "));
        // 业务唯一键防重：NOT EXISTS 同时按 code 判重，防同码不同 id 行撞 uk_code 令批次永久失败
        assertTrue(sql.contains("WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 1911"
                + " OR code = 'schedule:task:list')"));
    }

    @Test
    void bizKeyUpsertPurgesForeignTombstones() {
        // 回归：异 id 软删墓碑占码（uk 不看 deleted）会令改名 UPDATE / 同码重建 INSERT 撞唯一索引，
        // 批次 Duplicate entry 永久失败并 head-of-line 阻塞整个待扇出窗口——upsert 先清他 id 墓碑
        SysPermission permission = new SysPermission();
        permission.setId(1911L);
        permission.setModule("t03x");
        permission.setName("改名联动");
        permission.setCode("t03x:perm:new-code");
        permission.setSort(1);
        permission.setStatus(0);
        String sql = CatalogChangeSqlRenderer.renderUpsert(permission);
        // 清障语句在最前：仅删同码、异 id、已逻辑删的占码行（同 id 墓碑保持删除语义不变）
        assertTrue(sql.startsWith("DELETE FROM sys_permission WHERE code = 't03x:perm:new-code'"
                        + " AND id <> 1911 AND deleted = 1;\n"),
                "upsert 须前置异 id 墓碑清障语句：" + sql);
        // 同 id 墓碑不受清障影响：DELETE 的 id<> 自排除 + UPDATE deleted=0 落空 + INSERT NOT EXISTS 挡住
        assertTrue(sql.contains("UPDATE sys_permission SET"));
        assertTrue(sql.contains("WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = 1911"
                + " OR code = 't03x:perm:new-code')"));
        // 幂等：同快照两次渲染逐字节一致
        assertEquals(sql, CatalogChangeSqlRenderer.renderUpsert(permission));
    }

    @Test
    void insertNullColumnsRenderNullNotNow() {
        // 回归：可空业务列在 INSERT 分支必须渲染 NULL（此前全列缺省 NOW()，menu_id 被写成时间戳，
        // 按 menu_id IS NULL 过滤的通用权限在租户副本中失效）
        SysPermission permission = new SysPermission();
        permission.setId(1L);
        permission.setCode("t:null");
        permission.setStatus(0);
        String sql = CatalogChangeSqlRenderer.renderUpsert(permission);
        String insertSection = sql.substring(sql.indexOf("SELECT "));
        assertTrue(insertSection.startsWith("SELECT 1, NULL, NULL, NULL, 't:null', NULL, 0, NULL, NULL, NOW(), NULL, NOW(), NULL FROM DUAL"),
                "可空业务列须渲染 NULL，仅时间列缺省 NOW()：" + insertSection);
    }

    @Test
    void microAppUpsertAndDelete() {
        SysMicroApp app = new SysMicroApp();
        app.setId(7L);
        app.setName("ai");
        app.setTitle("AI 平台");
        app.setDevEntry(null);
        app.setProdEntry("http://localhost:4300/");
        app.setSide(SysMicroApp.SIDE_BOTH);
        app.setClient(SysMenu.CLIENT_PC);
        app.setSort(0);
        app.setStatus(0);
        String sql = CatalogChangeSqlRenderer.renderUpsert(app);
        assertTrue(sql.contains("prod_entry = 'http://localhost:4300/'"));
        // 业务唯一键防重：NOT EXISTS 同时按 name 判重（uk_micro_app_name）
        assertTrue(sql.contains("WHERE NOT EXISTS (SELECT 1 FROM sys_micro_app WHERE id = 7 OR name = 'ai')"));
        // 带业务键的 upsert 同样前置异 id 墓碑清障
        assertTrue(sql.startsWith("DELETE FROM sys_micro_app WHERE name = 'ai' AND id <> 7 AND deleted = 1;\n"));
        assertEquals("UPDATE sys_micro_app SET deleted = 1 WHERE id = 7;",
                CatalogChangeSqlRenderer.renderDelete("sys_micro_app", 7L));
    }
}
