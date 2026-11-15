-- 测试夹具：每租户通道（种子，纯 DML；表结构归结构脚本/克隆管）
-- mse-per-tenant: true
INSERT INTO oa_seed VALUES (1, ${tenant_id})
