-- mse-target: platform
-- =====================================================================
-- 交付附件签发（ADR-9 职责归位：平台只签发附件包，最终交付包由 deliver-studio
-- 「套件 + 附件」重打包产出）——
--   1) 删运行时套件登记表 sys_deliver_kit（套件唯一真源=deliver-studio，平台不再存套件 zip）；
--   2) sys_deliver_package 改造为「附件签发记录」台账：新增模块集/直推记录号/推送状态列，
--      删套件关联列（kit_id/app_mode/include_images——形态与中间件勾选早已前移 studio 打包时）；
--   3) 权限点收敛：删套件查询/登记维护（3414/3415）；菜单 350 改名「交付附件签发」
-- 幂等：账本单次应用保证 DDL；种子按条件更新/删除重复执行无害
-- =====================================================================

DROP TABLE IF EXISTS sys_deliver_kit;

ALTER TABLE sys_deliver_package
    ADD COLUMN module_list VARCHAR(500) NULL COMMENT '签发模块集（逗号分隔；空=全量不过滤）' AFTER license_id,
    ADD COLUMN studio_delivery_id VARCHAR(64) NULL COMMENT '直推 deliver-studio 的附件记录号' AFTER module_list,
    ADD COLUMN push_status VARCHAR(200) NULL COMMENT '推送状态：pending/pushed/failed: 原因；空=未请求推送' AFTER studio_delivery_id,
    DROP COLUMN kit_id,
    DROP COLUMN app_mode,
    DROP COLUMN include_images;

ALTER TABLE sys_deliver_package COMMENT ='交付附件签发记录（记录行即异步签发任务；平台只签发，最终交付包由 deliver-studio 重打包）';

-- 套件管理权限点退役（deliver:kit:list/manage）
DELETE FROM sys_permission WHERE id IN (3414, 3415) OR code IN ('deliver:kit:list', 'deliver:kit:manage');

-- 菜单 350 更名（路径/组件不变；幂等：仅旧名时改）
UPDATE sys_menu SET name = '交付附件签发', update_time = NOW()
WHERE id = 350 AND name = '离线交付';
