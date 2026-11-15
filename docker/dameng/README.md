# 达梦 DM8 环境与初始化

达梦官方不提供公开 Docker 镜像仓库,需从[达梦官网](https://eco.dameng.com/download/)获取:

- **Docker 部署**:下载对应架构的镜像 tar(如 `dm8_xxx_docker_image.tar`)后 `docker load -i xxx.tar`,
  参考官方《DM8 Docker 部署手册》启动(默认端口 5236,默认账号 SYSDBA/SYSDBA001,具体以镜像说明为准)。
- **本机安装**:安装 DM8(初始化实例时**字符集选 UTF-8**;页大小/日志文件按默认即可;
  大小写是否敏感均可——DDL 与应用 SQL 均使用未加引号标识符,会统一按库级规则折叠,无需特殊处理)。

## 初始化（零手工）

基座/平台库三方言结构与种子已版本化进 `service-system` / `service-platform` jar
（`db/dm/`，mse-target: base / platform），随应用启动自动应用（首次启动会自动建
schema 所需对象，`mse.tenant.schema.auto-migrate` 默认开启，失败阻断启动）——
**无需手工执行任何初始化 SQL**（原 `init/01_schema.sql`、`init/02_data.sql` 已删除）。

业务工程同样零手工：业务表随业务 jar 内 `db/<方言>/` 版本化脚本（缺省
mse-target: master）启动自动应用；对基座库的注册数据（子应用/菜单/权限点，
mse-target: base）随 jar 经 starter-bridge 自动推送 mse-system 应用；独立库租户
schema（如 MSE_TENANT2）由平台端「开通租户 → 租户数据源初始化/懒初始化」克隆自动就绪。

## 应用侧切换

驱动已随工程打包(`com.dameng:DmJdbcDriver18`,版本见 `mse-dependencies/pom.xml`)。
启动时加 profile 即可,**无需改代码/重打包**:

```bash
java -jar mse-monolith.jar --spring.profiles.active=dm
# 连接信息默认 localhost:5236 / SYSDBA / SYSDBA001,可用环境变量覆盖:
#   MSE_DM_HOST / MSE_DM_PORT / MSE_DM_USER / MSE_DM_PASSWORD
```

与 MySQL 的库映射:`mse_platform`(基座/平台主库,首启自动建库)→ schema `MSE`;
独立库租户(如 `mse_tenant2`)→ schema `MSE_TENANT2`,经「租户数据源」界面登记后动态建库。

## 注意事项

- **分页方言**:`--spring.profiles.active=dm` 已显式设置 `mse.mybatis.db-type: dm`
  (MyBatis-Plus 按达梦 Oracle 风格分页);不设时默认 `auto` 也能按 `jdbc:dm:` URL 自动识别。
- **Seata**(默认关闭):undo_log 表已建;启用前需自行验证 Seata 2.6 对达梦方言的支持
  (参见根 README「已知限制」)。
- **驱动版本**:BOM 默认 8.1.3.140(Maven Central);若目标环境要求匹配特定 DM 服务端小版本,
  可在 `mse-dependencies/pom.xml` 调整 `dameng-jdbc.version` 或构建时 `-Ddameng-jdbc.version=...` 覆盖。
