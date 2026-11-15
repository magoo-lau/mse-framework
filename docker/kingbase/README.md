# 人大金仓 KingbaseES 环境与初始化

金仓官方不提供公开 Docker 镜像仓库,需从[电科金仓官网](https://www.kingbase.com.cn/download.html)获取:

- **Docker 部署**:下载 KingbaseES V008R00x 系列 Docker 镜像 tar 后 `docker load` 导入并启动
  (默认端口 54321,默认账号以镜像说明为准,常见 system/123456)。
- **本机安装**:安装 KingbaseES V8R6 及以上(兼容模式 Oracle/MySQL/PG 均可——本项目 SQL 已做
  方言无关化处理;初始化时字符集选 UTF-8)。

**驱动版本必须与服务端大版本匹配**:工程默认 `kingbase8:8.6.0`(对应 V8R6);
V9 环境请构建时覆盖 `-Dkingbase-jdbc.version=9.0.1`(或在 `mse-dependencies/pom.xml` 修改属性)。

## 初始化（零手工）

基座/平台库三方言结构与种子已版本化进 `service-system` / `service-platform` jar
（`db/kingbase/`，mse-target: base / platform），随应用启动自动应用
（`mse.tenant.schema.auto-migrate` 默认开启，失败阻断启动）——**无需手工执行任何
初始化 SQL**（原 `init/01_schema.sql`、`init/02_data.sql` 已删除）。

业务工程同样零手工：业务表随业务 jar 内 `db/<方言>/` 版本化脚本（缺省
mse-target: master）启动自动应用；对基座库的注册数据（子应用/菜单/权限点，
mse-target: base）随 jar 经 starter-bridge 自动推送 mse-system 应用；独立库租户
database（如 mse_tenant2）由平台端「开通租户 → 租户数据源初始化/懒初始化」克隆自动就绪。

## 应用侧切换

驱动已随工程打包(`cn.com.kingbase:kingbase8`,版本见 `mse-dependencies/pom.xml`)。
启动时加 profile 即可,**无需改代码/重打包**:

```bash
java -jar mse-monolith.jar --spring.profiles.active=kingbase
# 连接信息默认 localhost:54321 / system / 123456,可用环境变量覆盖:
#   MSE_KB_HOST / MSE_KB_PORT / MSE_KB_USER / MSE_KB_PASSWORD / MSE_KB_DB / MSE_KB_DB_TENANT2
```

与 MySQL 的库映射:`mse_platform`(基座/平台主库,首启自动建库)→ database `mse` + schema `mse`;
独立库租户(如 `mse_tenant2`)→ 独立 database `mse_tenant2` + schema `mse_tenant2`,经「租户数据源」界面登记后动态建库。

## 注意事项

- **分页方言**:`--spring.profiles.active=kingbase` 已显式设置 `mse.mybatis.db-type: kingbase_es`
  (MyBatis-Plus 按 PostgreSQL 风格 LIMIT/OFFSET 分页);不设时默认 `auto` 也能按
  `jdbc:kingbase8:` URL 自动识别。
- **Seata**(默认关闭):undo_log 表已建;启用前需自行验证 Seata 2.6 对金仓方言的支持
  (参见根 README「已知限制」)。
