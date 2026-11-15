package vip.mseapp.mybatis.slowsql;

/**
 * 单条慢 SQL 记录（时间为展示用格式化字符串，SQL 已截断，仅静态文本不含参数值）。
 */
public record SlowSqlRecord(String time, long elapsedMs, String mapperId, String sql) {
}
