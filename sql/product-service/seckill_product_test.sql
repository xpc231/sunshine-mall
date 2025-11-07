-- ==========================================
-- 秒杀商品测试数据
-- ==========================================
-- 说明：为秒杀商品API测试提供数据，每个接口提供两组测试数据
-- 注意：需要先执行 test.sql 中的商品和SKU数据，确保 product_id 和 sku_id 存在

-- 清空现有测试数据（可选）
-- DELETE FROM `seckill_product` WHERE id IN (100001, 100002, 100003, 100004, 100005, 100006);

-- ==========================================
-- 测试数据1：进行中的秒杀商品（status=1）
-- ==========================================

-- 测试数据1-1：iPhone 15 Pro Max 原色钛金属 256GB 秒杀
-- 用途：进行中的秒杀，用于测试查询、扣减库存、预热库存等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100001,                                    -- 秒杀商品ID
    1001,                                      -- 商品ID（iPhone 15 Pro Max）
    10001,                                     -- SKU ID（原色钛金属 256GB）
    7999.00,                                   -- 秒杀价格（原价9999，秒杀价7999，优惠2000）
    100,                                       -- 秒杀库存（100件）
    9999.00,                                   -- 原价
    DATE_SUB(NOW(), INTERVAL 1 HOUR),          -- 开始时间（1小时前）
    DATE_ADD(NOW(), INTERVAL 23 HOUR),        -- 结束时间（23小时后）
    2,                                         -- 限购数量（每人限购2件）
    1,                                         -- 状态（1-进行中）
    1,                                         -- 排序顺序
    NOW(),                                     -- 创建时间
    NOW(),                                     -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- 测试数据1-2：华为 Mate 60 Pro 雅丹黑 12GB+512GB 秒杀
-- 用途：进行中的秒杀，用于测试查询、扣减库存、预热库存等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100002,                                    -- 秒杀商品ID
    1002,                                      -- 商品ID（华为 Mate 60 Pro）
    10005,                                     -- SKU ID（雅丹黑 12GB+512GB）
    5999.00,                                   -- 秒杀价格（原价6999，秒杀价5999，优惠1000）
    50,                                        -- 秒杀库存（50件）
    6999.00,                                   -- 原价
    DATE_SUB(NOW(), INTERVAL 2 HOUR),          -- 开始时间（2小时前）
    DATE_ADD(NOW(), INTERVAL 22 HOUR),        -- 结束时间（22小时后）
    1,                                         -- 限购数量（每人限购1件）
    1,                                         -- 状态（1-进行中）
    2,                                         -- 排序顺序
    NOW(),                                     -- 创建时间
    NOW(),                                     -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- ==========================================
-- 测试数据2：未开始的秒杀商品（status=0）
-- ==========================================

-- 测试数据2-1：小米14 Pro 黑色 12GB+256GB 秒杀（未开始）
-- 用途：用于测试查询未开始的秒杀、更新秒杀商品等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100003,                                    -- 秒杀商品ID
    1003,                                      -- 商品ID（小米14 Pro）
    10007,                                     -- SKU ID（黑色 12GB+256GB）
    3999.00,                                   -- 秒杀价格（原价4999，秒杀价3999，优惠1000）
    200,                                       -- 秒杀库存（200件）
    4999.00,                                   -- 原价
    DATE_ADD(NOW(), INTERVAL 1 DAY),           -- 开始时间（1天后）
    DATE_ADD(NOW(), INTERVAL 2 DAY),           -- 结束时间（2天后）
    3,                                         -- 限购数量（每人限购3件）
    0,                                         -- 状态（0-未开始）
    3,                                         -- 排序顺序
    NOW(),                                     -- 创建时间
    NOW(),                                     -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- 测试数据2-2：MacBook Pro M3 Pro 18GB+512GB 秒杀（未开始）
-- 用途：用于测试查询未开始的秒杀、更新秒杀商品等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100004,                                    -- 秒杀商品ID
    2001,                                      -- 商品ID（MacBook Pro）
    20001,                                     -- SKU ID（M3 Pro 18GB+512GB）
    12999.00,                                  -- 秒杀价格（原价14999，秒杀价12999，优惠2000）
    30,                                        -- 秒杀库存（30件）
    14999.00,                                  -- 原价
    DATE_ADD(NOW(), INTERVAL 3 DAY),           -- 开始时间（3天后）
    DATE_ADD(NOW(), INTERVAL 4 DAY),          -- 结束时间（4天后）
    1,                                         -- 限购数量（每人限购1件）
    0,                                         -- 状态（0-未开始）
    4,                                         -- 排序顺序
    NOW(),                                     -- 创建时间
    NOW(),                                     -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- ==========================================
-- 测试数据3：已结束的秒杀商品（status=2）
-- ==========================================

-- 测试数据3-1：ThinkPad X1 Carbon 秒杀（已结束）
-- 用途：用于测试查询已结束的秒杀、分页查询等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100005,                                    -- 秒杀商品ID
    2002,                                      -- 商品ID（ThinkPad X1 Carbon）
    20004,                                     -- SKU ID（i7-1365U 16GB+512GB）
    8999.00,                                   -- 秒杀价格（原价9999，秒杀价8999，优惠1000）
    0,                                         -- 秒杀库存（已售罄）
    9999.00,                                   -- 原价
    DATE_SUB(NOW(), INTERVAL 3 DAY),           -- 开始时间（3天前）
    DATE_SUB(NOW(), INTERVAL 1 DAY),           -- 结束时间（1天前）
    2,                                         -- 限购数量（每人限购2件）
    2,                                         -- 状态（2-已结束）
    5,                                         -- 排序顺序
    DATE_SUB(NOW(), INTERVAL 4 DAY),           -- 创建时间
    DATE_SUB(NOW(), INTERVAL 1 DAY),           -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- 测试数据3-2：女士羊毛大衣 驼色 S码 秒杀（已结束）
-- 用途：用于测试查询已结束的秒杀、分页查询等接口
INSERT INTO `seckill_product` (
    `id`, `product_id`, `sku_id`, `seckill_price`, `seckill_stock`, `original_price`,
    `start_time`, `end_time`, `limit_quantity`, `status`, `sort_order`,
    `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `version`
) VALUES (
    100006,                                    -- 秒杀商品ID
    4002,                                      -- 商品ID（女士羊毛大衣）
    40005,                                     -- SKU ID（驼色 S码）
    999.00,                                    -- 秒杀价格（原价1299，秒杀价999，优惠300）
    5,                                         -- 秒杀库存（剩余5件）
    1299.00,                                   -- 原价
    DATE_SUB(NOW(), INTERVAL 5 DAY),           -- 开始时间（5天前）
    DATE_SUB(NOW(), INTERVAL 2 DAY),           -- 结束时间（2天前）
    1,                                         -- 限购数量（每人限购1件）
    2,                                         -- 状态（2-已结束）
    6,                                         -- 排序顺序
    DATE_SUB(NOW(), INTERVAL 6 DAY),           -- 创建时间
    DATE_SUB(NOW(), INTERVAL 2 DAY),           -- 更新时间
    'admin',                                   -- 创建人
    'admin',                                   -- 更新人
    0,                                         -- 删除标识（0-未删除）
    0                                          -- 版本号（乐观锁）
);

-- ==========================================
-- 测试数据说明
-- ==========================================
-- 
-- 接口测试数据对应关系：
-- 
-- 1. POST /api/seckill/product (创建秒杀商品)
--    - 使用任意 product_id 和 sku_id（需确保存在）
--    - 测试数据：可参考上述任意一条数据格式
-- 
-- 2. PUT /api/seckill/product/{id} (更新秒杀商品)
--    - 测试数据1-1：id=100001（进行中的秒杀）
--    - 测试数据1-2：id=100002（进行中的秒杀）
-- 
-- 3. DELETE /api/seckill/product/{id} (删除秒杀商品)
--    - 测试数据2-1：id=100003（未开始的秒杀，可删除）
--    - 测试数据2-2：id=100004（未开始的秒杀，可删除）
-- 
-- 4. GET /api/seckill/product/{id} (根据ID获取)
--    - 测试数据1-1：id=100001
--    - 测试数据1-2：id=100002
-- 
-- 5. GET /api/seckill/product/sku/{skuId} (根据SKU ID获取)
--    - 测试数据1-1：skuId=10001
--    - 测试数据1-2：skuId=10005
-- 
-- 6. GET /api/seckill/product/page (分页查询)
--    - 所有测试数据（6条）
--    - 可测试按状态筛选：status=0（未开始）、status=1（进行中）、status=2（已结束）
-- 
-- 7. GET /api/seckill/product/in-progress (查询进行中的)
--    - 测试数据1-1：id=100001
--    - 测试数据1-2：id=100002
-- 
-- 8. POST /api/seckill/product/{id}/deduct-stock (扣减库存)
--    - 测试数据1-1：id=100001, quantity=1 或 2
--    - 测试数据1-2：id=100002, quantity=1
-- 
-- 9. POST /api/seckill/product/{id}/warmup-stock (预热库存)
--    - 测试数据1-1：id=100001
--    - 测试数据1-2：id=100002
-- 
-- 10. POST /api/seckill/product/{id}/rollback-stock (回滚库存)
--     - 测试数据1-1：id=100001, quantity=1（先扣减再回滚）
--     - 测试数据1-2：id=100002, quantity=1（先扣减再回滚）
-- 
-- ==========================================
-- 注意事项
-- ==========================================
-- 
-- 1. 执行前确保已执行 test.sql，确保 product_id 和 sku_id 存在
-- 2. 测试数据中的时间使用相对时间（NOW()），确保测试时数据状态正确
-- 3. 进行中的秒杀商品（status=1）用于测试扣减库存、预热库存等接口
-- 4. 未开始的秒杀商品（status=0）用于测试更新、删除等接口
-- 5. 已结束的秒杀商品（status=2）用于测试查询历史数据
-- 6. 版本号（version）初始值为0，每次更新会自动递增
-- 7. 删除标识（is_deleted）为0表示未删除，删除操作是逻辑删除
-- 
-- ==========================================

