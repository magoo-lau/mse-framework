-- mse-target: platform
-- =====================================================================
-- 内置任务调度登记加「任务分类」列（管理面「任务调度」页分域展示，人大金仓）
--   platform=平台任务（控制面：目录扇出/权益对账/基座与自注册推送等，平台运维全域对象）
--   biz=业务域任务（扫描/重试/采集等业务基础设施，缺省值）
-- 分类随代码上报收敛（@MseScheduled.category / TaskDefinition.category，缺省 biz）：
-- 历史 platform 类任务由下一个同步周期上报自愈纠正，无需回填。
-- ALTER 无条件写法（账本 mse_platform_schema_history 保证单次执行）。
-- =====================================================================
ALTER TABLE sys_schedule_task ADD COLUMN task_category VARCHAR(20) NOT NULL DEFAULT 'biz';
