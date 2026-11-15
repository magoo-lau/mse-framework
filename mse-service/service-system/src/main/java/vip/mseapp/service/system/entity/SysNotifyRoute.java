package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.mseapp.mybatis.data.BaseEntity;

/**
 * 通知路由配置（业务动作 → 投递渠道，租户隔离；框架级租户配置，基座系统管理「通知渠道设置」维护）。
 *
 * <p>解析链 {@code (biz,type)} 精确 → {@code (biz,*)} → {@code (*,*)} → 兜底 [inbox]
 * （未配置默认仅站内信）；业务方发送不携带渠道时由框架中枢按本表解析。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_route")
public class SysNotifyRoute extends BaseEntity {

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 0;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 1;

    /** 通配（任意业务/任意类型） */
    public static final String WILDCARD = "*";

    /** 业务来源码（已登记来源或 * 通配） */
    private String bizCode;

    /** 消息类型（来源内动作语义或 * 通配） */
    private String msgType;

    /** 投递渠道 CSV（渠道编码逗号分隔，如 inbox,im） */
    private String channels;

    /** 0 启用 / 1 停用（停用即不参与解析，回落通配/默认） */
    private Integer status;

    /** 备注 */
    private String remark;
}
