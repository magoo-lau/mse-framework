package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 基座库版本化脚本描述（业务积木向 mse-system 推送的 base 通道条目）。
 *
 * <p>业务积木把基座注册脚本（target=base：子应用注册/菜单/权限点等，内容幂等：
 * INSERT ... WHERE NOT EXISTS / CREATE TABLE IF NOT EXISTS / 条件 UPDATE）随 jar 放在
 * {@code db/<方言>/}，由 starter-bridge 推送器按 target=base 过滤后推送至 mse-system；
 * 接收端按自身方言筛取、经 mse_&lt;biz&gt;_schema_history 账本只应用 pending 脚本
 * （不重跑已应用、checksum 变化报冲突）。
 *
 * @param biz      服务标识（文件名 biz 段；账本路由 mse_&lt;biz&gt;_schema_history）
 * @param version  版本号（文件名 {@code V20260922_01_<biz>__<描述>.sql} → {@code 20260922_01}，
 *                 同 biz 内字典序即时间序）
 * @param dialect  方言（mysql/dm/kingbase，与 sys_tenant_datasource.db_type 一致）
 * @param script   脚本文件名（含路径，如 db/mysql/V20260922_01_mp__base.sql，审计与账本去重键）
 * @param sql      脚本内容（普通 DDL/DML，以 ; 切分；不含 USE/SET SCHEMA 等连接级语句）
 * @param checksum 规范化文本（\r 归一 + trim）SHA-256，用于检测已应用脚本被改动
 */
public class BaseScriptDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务标识（账本路由） */
    private String biz;

    /** 版本号（字典序即时间序） */
    private String version;

    /** 方言（mysql/dm/kingbase） */
    private String dialect;

    /** 脚本文件名（含路径，账本去重键 + 审计展示） */
    private String script;

    /** 脚本内容 */
    private String sql;

    /** 内容 checksum（SHA-256） */
    private String checksum;

    /** base 落库域（PLATFORM/TENANT，缺省 TENANT） */
    private String baseScope;

    public String getBiz() {
        return biz;
    }

    public void setBiz(String biz) {
        this.biz = biz;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getBaseScope() {
        return baseScope;
    }

    public void setBaseScope(String baseScope) {
        this.baseScope = baseScope;
    }
}
