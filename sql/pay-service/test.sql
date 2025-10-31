-- ==========================================
-- Pay Service 初始数据脚本（UTF-8）
-- 说明：可重复执行；使用占位符模拟渠道配置与典型交易/退款场景
-- ==========================================

START TRANSACTION;

-- 预清理（仅供测试环境使用）
DELETE FROM pay_notify_log WHERE ref_no IN ('PAY202501010002','PAY202501010004','PAY202501010005','PAY202501010006','PAY202501010007','REF202501020001');
DELETE FROM pay_refund WHERE refund_sn IN ('REF202501020001');
DELETE FROM pay_transaction WHERE pay_sn IN ('PAY202501010002','PAY202501010004','PAY202501010005','PAY202501010006','PAY202501010007');
DELETE FROM pay_channel_config WHERE channel IN (1,2);

-- 支付渠道配置（占位符，实际请使用环境变量或密钥管理系统）
INSERT INTO pay_channel_config (id, channel, app_id, mch_id, private_key, public_key, notify_url, status, create_time, update_time, is_deleted) VALUES
    (1, 1, 'ENV:ALIPAY_APP_ID', 'ENV:ALIPAY_MCH_ID', 'ENV:ALIPAY_PRIVATE_KEY', 'ENV:ALIPAY_PUBLIC_KEY', 'https://api.example.com/api/pay/callback/alipay', 1, NOW(), NOW(), 0),
    (2, 2, 'ENV:WECHAT_APP_ID', 'ENV:WECHAT_MCH_ID', 'ENV:WECHAT_PRIVATE_KEY', 'ENV:WECHAT_PUBLIC_KEY', 'https://api.example.com/api/pay/callback/wechat', 1, NOW(), NOW(), 0);

-- 交易示例（与 order-service 测试数据对应）
INSERT INTO pay_transaction (
  id, pay_sn, order_id, order_no, user_id, amount, currency, pay_type, status, subject, body, channel_trade_no,
  client_ip, expire_time, callback_time, create_time, update_time, create_by, update_by, is_deleted
) VALUES
  (20001, 'PAY202501010002', 10002, 'ORD202501010002', 20001, 288.00, 'CNY', 2, 1, '订单支付', '微信支付成功', 'TRADE202501010002', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'system', 'system', 0),
  (20002, 'PAY202501010004', 10004, 'ORD202501010004', 20001, 169.00, 'CNY', 1, 1, '订单支付', '支付宝支付成功', 'TRADE202501010004', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'system', 'system', 0),
  (20003, 'PAY202501010005', 10005, 'ORD202501010005', 20002, 299.00, 'CNY', 2, 1, '订单支付', '微信支付成功（后退款）', 'TRADE202501010005', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'system', 'system', 0),
  (20004, 'PAY202501010006', 10006, 'ORD202501010006', 20001, 199.00, 'CNY', 1, 2, '订单支付', '支付宝支付失败', 'TRADE202501010006', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'system', 'system', 0),
  (20005, 'PAY202501010007', 10007, 'ORD202501010007', 20001, 9.90,  'CNY', 2, 1, '订单支付', '微信支付成功（秒杀）', 'TRADE202501010007', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'system', 'system', 0);

-- 退款示例（与订单 10005 对应）
INSERT INTO pay_refund (
  id, refund_sn, pay_id, pay_sn, order_id, order_no, user_id, amount, status, reason, channel_refund_no,
  callback_time, create_time, update_time, is_deleted
) VALUES
  (30001, 'REF202501020001', 20003, 'PAY202501010005', 10005, 'ORD202501010005', 20002, 299.00, 1, '7天无理由退货', 'REFUND202501020001', NOW(), NOW(), NOW(), 0);

COMMIT;