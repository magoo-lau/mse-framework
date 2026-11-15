-- mse-target: platform
-- =====================================================================
-- 离线交付 format:2 切换（三段管道：collect-kit 构建 / deliver-studio 打包 / 平台交付）：
--   运行时套件 = deliver-studio 打包产出的 format:2 部署包（components.json 驱动），
--   平台不再解析 kit-manifest layout 1/2——新增组件清单列、清空旧布局套件登记（仅 demo 数据，无需兼容）
-- 幂等：账本单次应用保证 DDL；DELETE 全表重复执行无害
-- =====================================================================

ALTER TABLE sys_deliver_kit
    ADD COLUMN component_list VARCHAR(500) NULL COMMENT '组件清单（components.json components[].code，逗号分隔）' AFTER module_list,
    MODIFY COLUMN manifest_json MEDIUMTEXT NULL COMMENT 'components.json 声明快照（format:2：入口单元/单元清单/变量/组件接口）',
    MODIFY COLUMN module_list VARCHAR(500) NULL COMMENT '包含业务积木清单（逗号分隔；驱动数据表按模块裁剪导出）';

-- 旧布局（kit-manifest layout 1/2）套件登记清空：format:2 起套件必须由 deliver-studio 重新打包上传
DELETE FROM sys_deliver_kit;
