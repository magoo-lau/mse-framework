-- 测试夹具：platform 通道（表名取 SYS_ 前缀语义：真实控制面/平台域建表全为框架前缀，
-- 探针被扣成空集——platform 通道必须关闭自动基线，靠幂等收敛，见 MasterSchemaAutoMigration）
-- mse-target: platform
CREATE TABLE sys_platform_marker (id INT NOT NULL, PRIMARY KEY (id))
