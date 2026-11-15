package vip.mseapp.service.system.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.service.system.entity.SysCatalogChange;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.mapper.SysCatalogChangeMapper;

/**
 * 目录变更批次记录器：平台管理端六写方法（菜单/权限点/子应用的新增、修改、删除、启停）在
 * 写库成功后把全列快照渲染为幂等 SQL 落 {@code sys_catalog_change}（PlatformBaseEntity
 * 自动路由平台库，任意会话安全）。
 *
 * <p>记录失败不阻断管理端操作（error 级日志可见，追加同目标行的新编辑即可补偿）；
 * 实体为空（保存后回读失败等）静默跳过。
 */
public class CatalogChangeRecorder {

    private static final Logger log = LoggerFactory.getLogger(CatalogChangeRecorder.class);

    private final SysCatalogChangeMapper mapper;

    public CatalogChangeRecorder(SysCatalogChangeMapper mapper) {
        this.mapper = mapper;
    }

    public void recordUpsert(SysMenu menu) {
        if (menu == null || menu.getId() == null) {
            return;
        }
        save(SysCatalogChange.TABLE_MENU, menu.getId(), SysCatalogChange.OP_UPSERT,
                CatalogChangeSqlRenderer.renderUpsert(menu));
    }

    public void recordUpsert(SysPermission permission) {
        if (permission == null || permission.getId() == null) {
            return;
        }
        save(SysCatalogChange.TABLE_PERMISSION, permission.getId(), SysCatalogChange.OP_UPSERT,
                CatalogChangeSqlRenderer.renderUpsert(permission));
    }

    public void recordUpsert(SysMicroApp app) {
        if (app == null || app.getId() == null) {
            return;
        }
        save(SysCatalogChange.TABLE_MICRO_APP, app.getId(), SysCatalogChange.OP_UPSERT,
                CatalogChangeSqlRenderer.renderUpsert(app));
    }

    public void recordMenuDelete(Long id) {
        recordDelete(SysCatalogChange.TABLE_MENU, id);
    }

    public void recordPermissionDelete(Long id) {
        recordDelete(SysCatalogChange.TABLE_PERMISSION, id);
    }

    public void recordMicroAppDelete(Long id) {
        recordDelete(SysCatalogChange.TABLE_MICRO_APP, id);
    }

    private void recordDelete(String table, Long id) {
        if (id == null) {
            return;
        }
        save(table, id, SysCatalogChange.OP_DELETE, CatalogChangeSqlRenderer.renderDelete(table, id));
    }

    private void save(String table, Long rowId, int op, String sql) {
        try {
            SysCatalogChange change = new SysCatalogChange();
            change.setTargetTable(table);
            change.setRowId(rowId);
            change.setOp(op);
            change.setSqlText(sql);
            change.setStatus(SysCatalogChange.STATUS_PENDING);
            mapper.insert(change);
        } catch (Exception e) {
            log.error("[mse-catalog] 目录变更批次记录失败（编辑已生效，扇出缺失——同目标行的后续编辑会补偿）："
                    + "table={} rowId={} op={}", table, rowId, op, e);
        }
    }
}
