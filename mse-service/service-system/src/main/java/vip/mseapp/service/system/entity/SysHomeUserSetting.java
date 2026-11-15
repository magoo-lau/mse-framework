package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户门户首页个性化设置（全局表，(owner_type, tenant_id, user_id) 唯一）。
 *
 * <p>layout_json 结构：{"layout":[卡片...],"themeId":"...","darkThemeId":"..."}——用户在模板可选块范围内
 * 挑卡、摆放、选主题（亮/深外观模式各一套主题槽位）的结果；无记录 = 跟随模板默认。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_home_user_setting")
public class SysHomeUserSetting extends GlobalBaseEntity {

    /** 归属：租户用户 */
    public static final int OWNER_TENANT = 1;
    /** 归属：平台用户 */
    public static final int OWNER_PLATFORM = 2;

    /** 归属 1 租户用户 2 平台用户 */
    private Integer ownerType;

    /** 租户编号（owner=1 时为本租户；owner=2 恒为 0） */
    private Long tenantId;

    private Long userId;

    /** 个性化 JSON：{"layout":[...],"themeId":"...","darkThemeId":"..."}（亮/深外观模式各一套主题槽位） */
    private String layoutJson;
}
