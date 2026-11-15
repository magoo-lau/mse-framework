package vip.mseapp.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 字段自动填充：tenant_id / create_by / create_time / update_by / update_time。
 *
 * <p>tenant_id 仅对声明了该属性的实体（继承 BaseEntity）填充；
 * 上下文缺失时填平台值 0——共享库模式下真正的防线在 MseTenantLineHandler 的快速失败。
 */
public class MseMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContext.getUserId();

        if (metaObject.hasSetter("tenantId") && metaObject.getValue("tenantId") == null) {
            Long tenantId = TenantContext.getTenantId();
            metaObject.setValue("tenantId", tenantId != null ? tenantId : MseConstants.PLATFORM_TENANT_ID);
        }
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (userId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = UserContext.getUserId();
        if (userId != null) {
            strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
