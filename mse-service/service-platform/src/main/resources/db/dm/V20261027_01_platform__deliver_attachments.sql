-- mse-target: platform
-- =====================================================================
-- 交付附件签发（ADR-9 职责归位：平台只签发附件包，最终交付包由 deliver-studio
-- 「套件 + 附件」重打包产出，达梦版）——
--   1) 删运行时套件登记表 sys_deliver_kit（套件唯一真源=deliver-studio）；
--   2) sys_deliver_package 改造为「附件签发记录」台账（新增模块集/直推记录号/推送状态列，
--      删套件关联列 kit_id/app_mode/include_images）；
--   3) 权限点收敛：删套件查询/登记维护（3414/3415）；菜单 350 改名「交付附件签发」
-- 幂等：账本单次应用保证 DDL；种子按条件更新/删除重复执行无害
-- =====================================================================

DROP TABLE IF EXISTS sys_deliver_kit;

ALTER TABLE sys_deliver_package ADD COLUMN module_list VARCHAR(500) NULL;
ALTER TABLE sys_deliver_package ADD COLUMN studio_delivery_id VARCHAR(64) NULL;
ALTER TABLE sys_deliver_package ADD COLUMN push_status VARCHAR(200) NULL;
ALTER TABLE sys_deliver_package DROP COLUMN kit_id;
ALTER TABLE sys_deliver_package DROP COLUMN app_mode;
ALTER TABLE sys_deliver_package DROP COLUMN include_images;

COMMENT ON TABLE sys_deliver_package IS '交付附件签发记录（记录行即异步签发任务；平台只签发，最终交付包由 deliver-studio 重打包）';
COMMENT ON COLUMN sys_deliver_package.module_list IS '签发模块集（逗号分隔；空=全量不过滤）';
COMMENT ON COLUMN sys_deliver_package.studio_delivery_id IS '直推 deliver-studio 的附件记录号';
COMMENT ON COLUMN sys_deliver_package.push_status IS '推送状态：pending/pushed/failed: 原因；空=未请求推送';

-- 套件管理权限点退役（deliver:kit:list/manage）
DELETE FROM sys_permission WHERE id IN (3414, 3415) OR code IN ('deliver:kit:list', 'deliver:kit:manage');

-- 菜单 350 更名（路径/组件不变；幂等：仅旧名时改）
UPDATE sys_menu SET name = '交付附件签发', update_time = CURRENT_TIMESTAMP
WHERE id = 350 AND name = '离线交付';
