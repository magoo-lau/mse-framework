-- mse-target: master
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：monitor 业务来源退役清理（MySQL，master 通道——业务主库+各租户独立库；
-- 平台库清理随 V20261020_02，租户路由种子清理随 mse-monitor 自有脚本）
-- 内容：monitor 告警恢复独立直发、退出通知中枢（v3.1），清理登记目录存量：
--       · sys_notify_channel 的 webhook 登记行（mse-monitor 提供，内置渠道接管，
--         物理删防墓碑撞键——uk 含 deleted，逻辑删后再登记同码必撞）
--       · sys_notify_source 的 monitor 来源行（业务来源下拉不再出现）
-- 幂等：DELETE 天然幂等，可重复执行
-- =====================================================================

DELETE FROM sys_notify_channel WHERE channel_code = 'webhook';
DELETE FROM sys_notify_source WHERE biz_code = 'monitor';
