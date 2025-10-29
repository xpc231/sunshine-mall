-- ==========================================
-- 商品测试数据
-- ==========================================

-- 1. 电子产品 - 手机类（3个商品）
INSERT INTO `product` (`id`, `category_id`, `product_name`, `product_code`, `brand_id`, `main_image`, `sub_images`, `detail`, `status`, `sale_count`, `view_count`) VALUES
                                                                                                                                                                        (1001, 111, 'iPhone 15 Pro Max', 'PHONE-APPLE-001', 1, 'https://cdn.example.com/iphone15-main.jpg', '["https://cdn.example.com/iphone15-1.jpg","https://cdn.example.com/iphone15-2.jpg","https://cdn.example.com/iphone15-3.jpg"]', '6.7英寸超视网膜XDR显示屏，A17 Pro芯片，钛金属设计，专业级摄像系统', 1, 1580, 25600),
                                                                                                                                                                        (1002, 111, '华为 Mate 60 Pro', 'PHONE-HUAWEI-001', 2, 'https://cdn.example.com/mate60-main.jpg', '["https://cdn.example.com/mate60-1.jpg","https://cdn.example.com/mate60-2.jpg"]', '6.82英寸OLED屏幕，麒麟9000S芯片，卫星通信，超光变摄像头', 1, 2340, 38900),
                                                                                                                                                                        (1003, 111, '小米14 Pro', 'PHONE-XIAOMI-001', 3, 'https://cdn.example.com/mi14-main.jpg', '["https://cdn.example.com/mi14-1.jpg","https://cdn.example.com/mi14-2.jpg","https://cdn.example.com/mi14-3.jpg"]', '6.73英寸2K屏，骁龙8 Gen3，徕卡光学镜头，120W快充', 1, 890, 15200),

-- 2. 电子产品 - 电脑类（2个商品）
                                                                                                                                                                        (2001, 121, 'MacBook Pro 14英寸', 'LAPTOP-APPLE-001', 1, 'https://cdn.example.com/macbook-main.jpg', '["https://cdn.example.com/macbook-1.jpg","https://cdn.example.com/macbook-2.jpg"]', 'M3 Pro芯片，14.2英寸Liquid视网膜XDR显示屏，18小时续航', 1, 560, 12300),
                                                                                                                                                                        (2002, 121, '联想ThinkPad X1 Carbon', 'LAPTOP-LENOVO-001', 4, 'https://cdn.example.com/thinkpad-main.jpg', '["https://cdn.example.com/thinkpad-1.jpg","https://cdn.example.com/thinkpad-2.jpg"]', '14英寸2.8K OLED触控屏，Intel酷睿Ultra 7，碳纤维机身，军工级品质', 1, 420, 8900),

-- 3. 服装鞋包 - 男装类（2个商品）
                                                                                                                                                                        (3001, 21, '男士商务衬衫', 'CLOTH-SHIRT-001', 5, 'https://cdn.example.com/shirt-main.jpg', '["https://cdn.example.com/shirt-1.jpg","https://cdn.example.com/shirt-2.jpg"]', '100%精梳棉，免烫处理，商务正装款式，多色可选', 1, 1200, 18500),
                                                                                                                                                                        (3002, 21, '男士休闲夹克', 'CLOTH-JACKET-001', 6, 'https://cdn.example.com/jacket-main.jpg', '["https://cdn.example.com/jacket-1.jpg","https://cdn.example.com/jacket-2.jpg","https://cdn.example.com/jacket-3.jpg"]', '防风防水面料，内里抓绒保暖，多口袋设计，户外休闲两用', 1, 780, 13400),

-- 4. 服装鞋包 - 女装类（2个商品）
                                                                                                                                                                        (4001, 22, '女士连衣裙', 'CLOTH-DRESS-001', 7, 'https://cdn.example.com/dress-main.jpg', '["https://cdn.example.com/dress-1.jpg","https://cdn.example.com/dress-2.jpg"]', '法式复古设计，高腰显瘦，真丝面料，适合春夏季节', 1, 950, 16800),
                                                                                                                                                                        (4002, 22, '女士羊毛大衣', 'CLOTH-COAT-001', 8, 'https://cdn.example.com/coat-main.jpg', '["https://cdn.example.com/coat-1.jpg","https://cdn.example.com/coat-2.jpg"]', '80%羊毛含量，经典H型版型，双面呢工艺，秋冬必备', 1, 620, 10200);

-- ==========================================
-- SKU测试数据
-- ==========================================

-- 1. iPhone 15 Pro Max SKU（3个规格）
INSERT INTO `product_sku` (`id`, `product_id`, `sku_code`, `sku_name`, `spec_json`, `price`, `original_price`, `cost_price`, `sku_image`, `weight`, `status`) VALUES
                                                                                                                                                                  (10001, 1001, 'SKU-IPHONE15-001', 'iPhone 15 Pro Max 原色钛金属 256GB', '{"颜色":"原色钛金属","内存":"256GB","版本":"国行"}', 9999.00, 9999.00, 7500.00, 'https://cdn.example.com/iphone15-natural.jpg', 0.22, 1),
                                                                                                                                                                  (10002, 1001, 'SKU-IPHONE15-002', 'iPhone 15 Pro Max 黑色钛金属 512GB', '{"颜色":"黑色钛金属","内存":"512GB","版本":"国行"}', 11999.00, 11999.00, 9200.00, 'https://cdn.example.com/iphone15-black.jpg', 0.22, 1),
                                                                                                                                                                  (10003, 1001, 'SKU-IPHONE15-003', 'iPhone 15 Pro Max 白色钛金属 1TB', '{"颜色":"白色钛金属","内存":"1TB","版本":"国行"}', 13999.00, 13999.00, 10800.00, 'https://cdn.example.com/iphone15-white.jpg', 0.22, 1),

-- 2. 华为 Mate 60 Pro SKU（3个规格）
                                                                                                                                                                  (10004, 1002, 'SKU-MATE60-001', '华为 Mate 60 Pro 雅川青 12GB+256GB', '{"颜色":"雅川青","内存":"12GB+256GB","版本":"5G"}', 6999.00, 6999.00, 5200.00, 'https://cdn.example.com/mate60-green.jpg', 0.23, 1),
                                                                                                                                                                  (10005, 1002, 'SKU-MATE60-002', '华为 Mate 60 Pro 雅丹黑 12GB+512GB', '{"颜色":"雅丹黑","内存":"12GB+512GB","版本":"5G"}', 7999.00, 7999.00, 6000.00, 'https://cdn.example.com/mate60-black.jpg', 0.23, 1),
                                                                                                                                                                  (10006, 1002, 'SKU-MATE60-003', '华为 Mate 60 Pro 白沙银 12GB+1TB', '{"颜色":"白沙银","内存":"12GB+1TB","版本":"5G"}', 8999.00, 8999.00, 6800.00, 'https://cdn.example.com/mate60-silver.jpg', 0.23, 1),

-- 3. 小米14 Pro SKU（3个规格）
                                                                                                                                                                  (10007, 1003, 'SKU-MI14-001', '小米14 Pro 黑色 12GB+256GB', '{"颜色":"黑色","内存":"12GB+256GB","版本":"5G"}', 4999.00, 4999.00, 3500.00, 'https://cdn.example.com/mi14-black.jpg', 0.22, 1),
                                                                                                                                                                  (10008, 1003, 'SKU-MI14-002', '小米14 Pro 白色 16GB+512GB', '{"颜色":"白色","内存":"16GB+512GB","版本":"5G"}', 5999.00, 5999.00, 4200.00, 'https://cdn.example.com/mi14-white.jpg', 0.22, 1),
                                                                                                                                                                  (10009, 1003, 'SKU-MI14-003', '小米14 Pro 钛金属 16GB+1TB', '{"颜色":"钛金属","内存":"16GB+1TB","版本":"5G"}', 6999.00, 6999.00, 5000.00, 'https://cdn.example.com/mi14-titan.jpg', 0.22, 1),

-- 4. MacBook Pro SKU（3个规格）
                                                                                                                                                                  (20001, 2001, 'SKU-MACBOOK-001', 'MacBook Pro 14 深空黑色 M3 Pro 18GB+512GB', '{"颜色":"深空黑色","芯片":"M3 Pro","内存":"18GB","硬盘":"512GB"}', 16999.00, 16999.00, 13000.00, 'https://cdn.example.com/macbook-black.jpg', 1.55, 1),
                                                                                                                                                                  (20002, 2001, 'SKU-MACBOOK-002', 'MacBook Pro 14 银色 M3 Pro 18GB+1TB', '{"颜色":"银色","芯片":"M3 Pro","内存":"18GB","硬盘":"1TB"}', 19999.00, 19999.00, 15200.00, 'https://cdn.example.com/macbook-silver.jpg', 1.55, 1),
                                                                                                                                                                  (20003, 2001, 'SKU-MACBOOK-003', 'MacBook Pro 14 深空黑色 M3 Max 36GB+1TB', '{"颜色":"深空黑色","芯片":"M3 Max","内存":"36GB","硬盘":"1TB"}', 25999.00, 25999.00, 20000.00, 'https://cdn.example.com/macbook-max.jpg', 1.60, 1),

-- 5. ThinkPad X1 Carbon SKU（3个规格）
                                                                                                                                                                  (20004, 2002, 'SKU-THINKPAD-001', 'ThinkPad X1 Carbon 经典黑 Ultra 7 16GB+512GB', '{"颜色":"经典黑","处理器":"Intel Ultra 7","内存":"16GB","硬盘":"512GB"}', 12999.00, 12999.00, 9500.00, 'https://cdn.example.com/thinkpad-black.jpg', 1.12, 1),
                                                                                                                                                                  (20005, 2002, 'SKU-THINKPAD-002', 'ThinkPad X1 Carbon 经典黑 Ultra 7 32GB+1TB', '{"颜色":"经典黑","处理器":"Intel Ultra 7","内存":"32GB","硬盘":"1TB"}', 15999.00, 15999.00, 12000.00, 'https://cdn.example.com/thinkpad-pro.jpg', 1.15, 1),
                                                                                                                                                                  (20006, 2002, 'SKU-THINKPAD-003', 'ThinkPad X1 Carbon 雷电灰 Ultra 9 32GB+2TB', '{"颜色":"雷电灰","处理器":"Intel Ultra 9","内存":"32GB","硬盘":"2TB"}', 19999.00, 19999.00, 15000.00, 'https://cdn.example.com/thinkpad-ultra.jpg', 1.18, 1),

-- 6. 男士商务衬衫 SKU（4个规格）
                                                                                                                                                                  (30001, 3001, 'SKU-SHIRT-001', '男士商务衬衫 白色 M码', '{"颜色":"白色","尺码":"M","袖长":"长袖"}', 299.00, 399.00, 120.00, 'https://cdn.example.com/shirt-white-m.jpg', 0.25, 1),
                                                                                                                                                                  (30002, 3001, 'SKU-SHIRT-002', '男士商务衬衫 白色 L码', '{"颜色":"白色","尺码":"L","袖长":"长袖"}', 299.00, 399.00, 120.00, 'https://cdn.example.com/shirt-white-l.jpg', 0.26, 1),
                                                                                                                                                                  (30003, 3001, 'SKU-SHIRT-003', '男士商务衬衫 蓝色 M码', '{"颜色":"蓝色","尺码":"M","袖长":"长袖"}', 299.00, 399.00, 120.00, 'https://cdn.example.com/shirt-blue-m.jpg', 0.25, 1),
                                                                                                                                                                  (30004, 3001, 'SKU-SHIRT-004', '男士商务衬衫 蓝色 XL码', '{"颜色":"蓝色","尺码":"XL","袖长":"长袖"}', 299.00, 399.00, 120.00, 'https://cdn.example.com/shirt-blue-xl.jpg', 0.27, 1),

-- 7. 男士休闲夹克 SKU（3个规格）
                                                                                                                                                                  (30005, 3002, 'SKU-JACKET-001', '男士休闲夹克 军绿色 L码', '{"颜色":"军绿色","尺码":"L","厚度":"加绒款"}', 599.00, 799.00, 280.00, 'https://cdn.example.com/jacket-green-l.jpg', 0.85, 1),
                                                                                                                                                                  (30006, 3002, 'SKU-JACKET-002', '男士休闲夹克 黑色 XL码', '{"颜色":"黑色","尺码":"XL","厚度":"加绒款"}', 599.00, 799.00, 280.00, 'https://cdn.example.com/jacket-black-xl.jpg', 0.88, 1),
                                                                                                                                                                  (30007, 3002, 'SKU-JACKET-003', '男士休闲夹克 卡其色 XXL码', '{"颜色":"卡其色","尺码":"XXL","厚度":"加绒款"}', 599.00, 799.00, 280.00, 'https://cdn.example.com/jacket-khaki-xxl.jpg', 0.92, 1),

-- 8. 女士连衣裙 SKU（4个规格）
                                                                                                                                                                  (40001, 4001, 'SKU-DRESS-001', '女士连衣裙 复古红 S码', '{"颜色":"复古红","尺码":"S","长度":"中长款"}', 688.00, 988.00, 320.00, 'https://cdn.example.com/dress-red-s.jpg', 0.35, 1),
                                                                                                                                                                  (40002, 4001, 'SKU-DRESS-002', '女士连衣裙 复古红 M码', '{"颜色":"复古红","尺码":"M","长度":"中长款"}', 688.00, 988.00, 320.00, 'https://cdn.example.com/dress-red-m.jpg', 0.36, 1),
                                                                                                                                                                  (40003, 4001, 'SKU-DRESS-003', '女士连衣裙 藏青色 S码', '{"颜色":"藏青色","尺码":"S","长度":"中长款"}', 688.00, 988.00, 320.00, 'https://cdn.example.com/dress-blue-s.jpg', 0.35, 1),
                                                                                                                                                                  (40004, 4001, 'SKU-DRESS-004', '女士连衣裙 米白色 L码', '{"颜色":"米白色","尺码":"L","长度":"中长款"}', 688.00, 988.00, 320.00, 'https://cdn.example.com/dress-white-l.jpg', 0.37, 1),

-- 9. 女士羊毛大衣 SKU（3个规格）
                                                                                                                                                                  (40005, 4002, 'SKU-COAT-001', '女士羊毛大衣 驼色 S码', '{"颜色":"驼色","尺码":"S","长度":"中长款"}', 1299.00, 1899.00, 650.00, 'https://cdn.example.com/coat-camel-s.jpg', 1.20, 1),
                                                                                                                                                                  (40006, 4002, 'SKU-COAT-002', '女士羊毛大衣 黑色 M码', '{"颜色":"黑色","尺码":"M","长度":"中长款"}', 1299.00, 1899.00, 650.00, 'https://cdn.example.com/coat-black-m.jpg', 1.25, 1),
                                                                                                                                                                  (40007, 4002, 'SKU-COAT-003', '女士羊毛大衣 灰色 L码', '{"颜色":"灰色","尺码":"L","长度":"中长款"}', 1299.00, 1899.00, 650.00, 'https://cdn.example.com/coat-gray-l.jpg', 1.28, 1);

-- ==========================================
-- 库存测试数据
-- ==========================================

-- 1. iPhone 15 Pro Max 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (10001, 500, 480, 20, 0),   -- 原色钛金属 256GB：热销款，库存充足
                                                                                                        (10002, 300, 285, 15, 0),   -- 黑色钛金属 512GB：高端款，库存中等
                                                                                                        (10003, 150, 145, 5, 0);    -- 白色钛金属 1TB：顶配款，库存较少

-- 2. 华为 Mate 60 Pro 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (10004, 600, 570, 30, 0),   -- 雅川青 12GB+256GB：畅销款
                                                                                                        (10005, 400, 375, 25, 0),   -- 雅丹黑 12GB+512GB：主力款
                                                                                                        (10006, 200, 190, 10, 0);   -- 白沙银 12GB+1TB：高配款

-- 3. 小米14 Pro 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (10007, 800, 750, 50, 0),   -- 黑色 12GB+256GB：主打款
                                                                                                        (10008, 500, 470, 30, 0),   -- 白色 16GB+512GB：高配款
                                                                                                        (10009, 250, 235, 15, 0);   -- 钛金属 16GB+1TB：旗舰款

-- 4. MacBook Pro 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (20001, 200, 185, 15, 0),   -- M3 Pro 18GB+512GB：入门款
                                                                                                        (20002, 150, 140, 10, 0),   -- M3 Pro 18GB+1TB：标准款
                                                                                                        (20003, 80, 75, 5, 0);      -- M3 Max 36GB+1TB：专业款

-- 5. ThinkPad X1 Carbon 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (20004, 300, 280, 20, 0),   -- Ultra 7 16GB+512GB：商务标配
                                                                                                        (20005, 180, 170, 10, 0),   -- Ultra 7 32GB+1TB：高端商务
                                                                                                        (20006, 100, 95, 5, 0);     -- Ultra 9 32GB+2TB：旗舰商务

-- 6. 男士商务衬衫 库存（快消品，库存量大）
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (30001, 1500, 1420, 80, 0),  -- 白色 M码：最畅销
                                                                                                        (30002, 1200, 1150, 50, 0),  -- 白色 L码：主力尺码
                                                                                                        (30003, 1000, 960, 40, 0),   -- 蓝色 M码：经典款
                                                                                                        (30004, 800, 770, 30, 0);    -- 蓝色 XL码：大码款

-- 7. 男士休闲夹克 库存
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (30005, 600, 565, 35, 0),    -- 军绿色 L码：热销色
                                                                                                        (30006, 700, 660, 40, 0),    -- 黑色 XL码：百搭款
                                                                                                        (30007, 400, 380, 20, 0);    -- 卡其色 XXL码：大码款

-- 8. 女士连衣裙 库存（应季商品）
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (40001, 800, 750, 50, 0),    -- 复古红 S码：热销款
                                                                                                        (40002, 1000, 940, 60, 0),   -- 复古红 M码：主力尺码
                                                                                                        (40003, 600, 570, 30, 0),    -- 藏青色 S码：经典色
                                                                                                        (40004, 500, 475, 25, 0);    -- 米白色 L码：清新款

-- 9. 女士羊毛大衣 库存（高端商品，库存适中）
INSERT INTO `product_stock` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
                                                                                                        (40005, 300, 280, 20, 0),    -- 驼色 S码：经典色
                                                                                                        (40006, 400, 375, 25, 0),    -- 黑色 M码：百搭款
                                                                                                        (40007, 250, 235, 15, 0);    -- 灰色 L码：商务款
