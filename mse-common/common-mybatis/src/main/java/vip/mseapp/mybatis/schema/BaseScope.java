package vip.mseapp.mybatis.schema;

/**
 * base 通道脚本的落库域（脚本头指令 {@code -- mse-base-scope: platform|tenant}，缺省 tenant）。
 *
 * <ul>
 *   <li>{@link #PLATFORM}：平台域——只落基座库（单体双库的 platform 数据源 / mse-system 微服务
 *       primary），如基座身份与平台菜单种子（service-system）、平台侧结构（monitor）；</li>
 *   <li>{@link #TENANT}：租户域——只落租户库车队（缺省共享主库 + 各独立库，各库自持
 *       sys_menu/sys_permission/sys_micro_app 副本与分册账本），如各业务积木的子应用注册/
 *       页面菜单/权限点脚本。</li>
 * </ul>
 * 注意：租户库的 BASE 通道重放是<b>全量</b>（两种 scope 都重放，与 standalone 形态同构、
 * 库可直接互换）；scope 只决定「基座库通道」是否应用该脚本——platform 域落基座库、
 * tenant 域完全不落基座库（避免平台库积攒各 biz 注册数据与账本）。
 */
public enum BaseScope {

    /** 平台域：仅基座库（MasterSchemaAutoMigration base 分支 / 推送接收端基座库通道） */
    PLATFORM,

    /** 租户域：仅租户库车队（共享主库 + 各独立库；启动 Runner / 推送接收端 / 懒初始化 BASE 通道） */
    TENANT;

    /** 宽松解析：null/空白/未知值回退租户域（历史脚本无指令即租户域语义） */
    public static BaseScope of(String value) {
        if (value == null || value.isBlank()) {
            return TENANT;
        }
        for (BaseScope scope : values()) {
            if (scope.name().equalsIgnoreCase(value.trim())) {
                return scope;
            }
        }
        return TENANT;
    }
}
