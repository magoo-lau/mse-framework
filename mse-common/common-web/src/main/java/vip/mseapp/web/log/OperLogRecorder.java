package vip.mseapp.web.log;

/**
 * 操作日志落库 SPI：由持有日志表的模块提供实现（当前 service-system）。
 *
 * <p>切面通过 Spring ObjectProvider 发现实现，未提供时操作日志静默跳过。
 * 实现方自行保证异步/容错，切面传入的事件不允许再修改。
 */
public interface OperLogRecorder {

    /**
     * 记录一条操作日志。
     *
     * @param event 切面采集的事件（含租户/用户/IP/参数/结果）
     */
    void record(OperLogEvent event);
}
