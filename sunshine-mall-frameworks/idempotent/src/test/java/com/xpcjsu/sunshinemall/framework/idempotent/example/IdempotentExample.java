package com.xpcjsu.sunshinemall.framework.idempotent.example;

import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.idempotent.exception.IdempotentException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性使用示例
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class IdempotentExample {

    /**
     * 示例服务类
     */
    @Service
    public static class OrderService {

        /**
         * 示例1：基础用法 - 订单创建
         */
        @Idempotent(key = "#orderId")
        public void createOrder(String orderId) {
            System.out.println("创建订单: " + orderId);
            // 业务逻辑
        }

        /**
         * 示例2：自定义消息 - 支付请求
         */
        @Idempotent(
            key = "#orderNo",
            message = "支付请求处理中，请勿重复操作"
        )
        public void pay(String orderNo, Double amount) {
            System.out.println("处理支付: " + orderNo + ", 金额: " + amount);
            // 业务逻辑
        }

        /**
         * 示例3：自定义前缀和过期时间 - 库存扣减
         */
        @Idempotent(
            key = "#productId + ':' + #userId",
            prefix = "inventory:deduct",
            expireTime = 30,
            message = "库存扣减处理中，请稍候"
        )
        public void deductStock(Long productId, Long userId, Integer quantity) {
            System.out.println("扣减库存: productId=" + productId + ", userId=" + userId + ", quantity=" + quantity);
            // 业务逻辑
        }

        /**
         * 示例4：对象参数 - 使用对象属性
         */
        @Idempotent(key = "#order.orderId")
        public void createOrderWithObject(Order order) {
            System.out.println("创建订单: " + order.getOrderId());
            // 业务逻辑
        }

        /**
         * 示例5：多参数组合 - 点赞操作
         */
        @Idempotent(
            key = "#userId + ':' + #articleId",
            prefix = "like",
            expireTime = 60,
            message = "您已点赞过该文章"
        )
        public void likeArticle(Long userId, Long articleId) {
            System.out.println("用户 " + userId + " 点赞文章 " + articleId);
            // 业务逻辑
        }

        /**
         * 示例6：自定义时间单位 - 短期幂等
         */
        @Idempotent(
            key = "#key",
            expireTime = 10,
            timeUnit = TimeUnit.SECONDS,
            message = "操作过于频繁，请稍后再试"
        )
        public void shortTermOperation(String key) {
            System.out.println("执行短期幂等操作: " + key);
            // 业务逻辑
        }
    }

    /**
     * 订单对象示例
     */
    public static class Order {
        private String orderId;
        private Long userId;
        private Double amount;

        public Order(String orderId, Long userId, Double amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }

        public String getOrderId() {
            return orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public Double getAmount() {
            return amount;
        }
    }

    /**
     * 使用示例主函数
     */
    public static void main(String[] args) {
        System.out.println("========== 幂等性使用示例 ==========\n");

        System.out.println("示例1：基础用法");
        System.out.println("@Idempotent(key = \"#orderId\")");
        System.out.println("public void createOrder(String orderId) {}\n");

        System.out.println("示例2：自定义消息");
        System.out.println("@Idempotent(");
        System.out.println("    key = \"#orderNo\",");
        System.out.println("    message = \"支付请求处理中，请勿重复操作\"");
        System.out.println(")");
        System.out.println("public void pay(String orderNo, Double amount) {}\n");

        System.out.println("示例3：自定义前缀和过期时间");
        System.out.println("@Idempotent(");
        System.out.println("    key = \"#productId + ':' + #userId\",");
        System.out.println("    prefix = \"inventory:deduct\",");
        System.out.println("    expireTime = 30");
        System.out.println(")");
        System.out.println("public void deductStock(Long productId, Long userId, Integer quantity) {}\n");

        System.out.println("示例4：对象参数");
        System.out.println("@Idempotent(key = \"#order.orderId\")");
        System.out.println("public void createOrderWithObject(Order order) {}\n");

        System.out.println("示例5：异常处理");
        System.out.println("try {");
        System.out.println("    orderService.createOrder(\"order-123\");");
        System.out.println("} catch (IdempotentException e) {");
        System.out.println("    System.out.println(\"检测到重复提交: \" + e.getMessage());");
        System.out.println("}\n");

        System.out.println("========== 幂等键格式示例 ==========\n");
        System.out.println("默认前缀 + 单参数:");
        System.out.println("  idempotent:order-123\n");
        
        System.out.println("默认前缀 + 多参数组合:");
        System.out.println("  idempotent:product-1:user-100\n");
        
        System.out.println("自定义前缀:");
        System.out.println("  inventory:deduct:product-1:user-100\n");
        
        System.out.println("对象属性:");
        System.out.println("  idempotent:order-123");
    }
}
