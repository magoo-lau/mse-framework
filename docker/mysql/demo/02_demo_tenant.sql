-- =====================================================================
-- 可选示例租户·控制面数据（手动导入，不属于类 flyway 初始化链路；平台库 mse_platform）
--   内容：示例租户 demo 注册（sys_tenant，共享库模式 ds_key=NULL，tenant_id=1）
--         + 域名个性化（sys_domain_config，demo.mse.local 登录页青绿渐变）
--   导入：mysql -h127.0.0.1 -uroot -p mse_platform < demo/02_demo_tenant.sql
--   依赖：mse-platform 已启动（platform 通道自动建控制面表 + 基础套餐 id=1 种子）
--   配套：租户域演示数据见 03_demo_tenant_identity.sql（导入缺省共享库 mse_tenant，Seeder 播种就绪后）
-- =====================================================================
USE mse_platform;
-- 示例租户（共享库模式：ds_key 为 NULL；预置绑定域名 demo.mse.local）
INSERT INTO sys_tenant (id, code, name, package_id, ds_key, domain, contact_user_name, status, expire_time, remark, create_time, update_time)
VALUES (1, 'demo', '示例租户', 1, NULL, 'demo.mse.local', '张三', 0, '2030-12-31 23:59:59', '共享库租户', NOW(), NOW());

-- 域名个性化示例：demo.mse.local 登录页换标题与配色（hosts 映射后访问即可看到效果）
INSERT INTO sys_domain_config (id, domain, site_title, site_logo, login_background, background_color, copyright, status, remark, create_time, update_time)
VALUES (1, 'demo.mse.local', '示例租户专属门户', NULL, NULL,
        'linear-gradient(135deg, #0ba360 0%, #3cba92 100%)', '© 2026 示例租户 · All Rights Reserved', 0,
        '域名个性化演示（青绿渐变）', NOW(), NOW());
