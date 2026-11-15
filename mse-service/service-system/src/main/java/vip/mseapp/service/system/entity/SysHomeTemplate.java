package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门户首页模板（全局表，两端物理分库、相互独立）：平台库份为平台模板（tenant_id=0 单行），
 * 各租户库份为租户模板（每租户一行，唯一键 tenant_id）。无跨端级联：任一端无配置（或停用）时
 * 前端回退内置默认（零配置起步）。用户个性化仍跟随本端生效模板。
 *
 * <p>layout_json 结构：{"layout":[默认布局卡片...],"catalog":[可选块目录...],
 * "themes":[主题...],"defaultThemeId":"...","defaultDarkThemeId":"..."}。管理员在「首页配置」维护。
 * 前端渲染器与卡片结构约定见 mse-ui/src/home/types.ts。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_home_template")
public class SysHomeTemplate extends GlobalBaseEntity {

    /** 租户编号（平台库份恒为 0；租户库份为本租户） */
    private Long tenantId;

    private String name;

    /** 布局 JSON：{"layout":[...],"catalog":[...],"themes":[...],"defaultThemeId":"...","defaultDarkThemeId":"..."} */
    private String layoutJson;

    /** 0 启用 1 停用（停用视为无配置，回退前端内置默认） */
    private Integer status;

    private String remark;
}
