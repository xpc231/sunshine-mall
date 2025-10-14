package com.xpcjsu.sunshinemall.framework.convention.errorcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessErrorCode错误码测试类
 * <p>
 * 专注于测试电商业务错误码的核心功能。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class BusinessErrorCodeTest {

    @Test
    void testUserErrorCodes() {
        // 验证用户相关错误码
        assertEquals("USER_NOT_FOUND", BusinessErrorCode.USER_NOT_FOUND);
        assertEquals("USER_NOT_LOGIN", BusinessErrorCode.USER_NOT_LOGIN);
        assertEquals("USER_ACCESS_DENIED", BusinessErrorCode.USER_ACCESS_DENIED);
    }

    @Test
    void testProductErrorCodes() {
        // 验证商品相关错误码
        assertEquals("PRODUCT_NOT_FOUND", BusinessErrorCode.PRODUCT_NOT_FOUND);
        assertEquals("PRODUCT_INSUFFICIENT_STOCK", BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        assertEquals("PRODUCT_OFFLINE", BusinessErrorCode.PRODUCT_OFFLINE);
    }

    @Test
    void testOrderErrorCodes() {
        // 验证订单相关错误码
        assertEquals("ORDER_NOT_FOUND", BusinessErrorCode.ORDER_NOT_FOUND);
        assertEquals("ORDER_STATUS_ERROR", BusinessErrorCode.ORDER_STATUS_ERROR);
        assertEquals("ORDER_CANCELLED", BusinessErrorCode.ORDER_CANCELLED);
    }

    @Test
    void testPaymentErrorCodes() {
        // 验证支付相关错误码
        assertEquals("PAYMENT_FAILED", BusinessErrorCode.PAYMENT_FAILED);
        assertEquals("PAYMENT_TIMEOUT", BusinessErrorCode.PAYMENT_TIMEOUT);
        assertEquals("PAYMENT_INSUFFICIENT_BALANCE", BusinessErrorCode.PAYMENT_INSUFFICIENT_BALANCE);
    }

    @Test
    void testSystemErrorCodes() {
        // 验证系统相关错误码
        assertEquals("SYSTEM_BUSY", BusinessErrorCode.SYSTEM_BUSY);
        assertEquals("SYSTEM_PARAM_ERROR", BusinessErrorCode.SYSTEM_PARAM_ERROR);
        assertEquals("SYSTEM_ERROR", BusinessErrorCode.SYSTEM_ERROR);
    }

    @Test
    void testUserDefaultMessages() {
        // 验证用户相关默认消息
        assertEquals("用户不存在", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_FOUND));
        assertEquals("用户未登录", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_LOGIN));
        assertEquals("用户权限不足", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_ACCESS_DENIED));
    }

    @Test
    void testProductDefaultMessages() {
        // 验证商品相关默认消息
        assertEquals("商品不存在", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PRODUCT_NOT_FOUND));
        assertEquals("商品库存不足", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK));
        assertEquals("商品已下架", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PRODUCT_OFFLINE));
    }

    @Test
    void testOrderDefaultMessages() {
        // 验证订单相关默认消息
        assertEquals("订单不存在", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.ORDER_NOT_FOUND));
        assertEquals("订单状态错误", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.ORDER_STATUS_ERROR));
        assertEquals("订单已取消", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.ORDER_CANCELLED));
    }

    @Test
    void testPaymentDefaultMessages() {
        // 验证支付相关默认消息
        assertEquals("支付失败", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PAYMENT_FAILED));
        assertEquals("支付超时", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PAYMENT_TIMEOUT));
        assertEquals("余额不足", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.PAYMENT_INSUFFICIENT_BALANCE));
    }

    @Test
    void testSystemDefaultMessages() {
        // 验证系统相关默认消息
        assertEquals("系统繁忙，请稍后重试", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.SYSTEM_BUSY));
        assertEquals("参数错误", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.SYSTEM_PARAM_ERROR));
        assertEquals("系统异常", BusinessErrorCode.getDefaultMessage(BusinessErrorCode.SYSTEM_ERROR));
    }

    @Test
    void testUnknownErrorCode() {
        // 验证未知错误码
        assertEquals("未知错误", BusinessErrorCode.getDefaultMessage("UNKNOWN_ERROR"));
        assertEquals("未知错误", BusinessErrorCode.getDefaultMessage(""));
        assertEquals("未知错误", BusinessErrorCode.getDefaultMessage(null));
    }

    @Test
    void testErrorCodeFormat() {
        // 验证错误码格式符合规范（模块_场景_错误类型）
        assertTrue(BusinessErrorCode.USER_NOT_FOUND.startsWith("USER_"));
        assertTrue(BusinessErrorCode.PRODUCT_NOT_FOUND.startsWith("PRODUCT_"));
        assertTrue(BusinessErrorCode.ORDER_NOT_FOUND.startsWith("ORDER_"));
        assertTrue(BusinessErrorCode.PAYMENT_FAILED.startsWith("PAYMENT_"));
        assertTrue(BusinessErrorCode.SYSTEM_ERROR.startsWith("SYSTEM_"));
    }

    @Test
    void testUtilityClassCannotBeInstantiated() {
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Constructor<BusinessErrorCode> constructor = BusinessErrorCode.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }

    @Test
    void testBusinessScenarios() {
        // 电商业务场景测试
        
        // 购物车场景：商品库存不足
        String stockError = BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK;
        String stockMessage = BusinessErrorCode.getDefaultMessage(stockError);
        assertEquals("PRODUCT_INSUFFICIENT_STOCK", stockError);
        assertEquals("商品库存不足", stockMessage);
        
        // 下单场景：用户未登录
        String loginError = BusinessErrorCode.USER_NOT_LOGIN;
        String loginMessage = BusinessErrorCode.getDefaultMessage(loginError);
        assertEquals("USER_NOT_LOGIN", loginError);
        assertEquals("用户未登录", loginMessage);
        
        // 支付场景：余额不足
        String balanceError = BusinessErrorCode.PAYMENT_INSUFFICIENT_BALANCE;
        String balanceMessage = BusinessErrorCode.getDefaultMessage(balanceError);
        assertEquals("PAYMENT_INSUFFICIENT_BALANCE", balanceError);
        assertEquals("余额不足", balanceMessage);
        
        // 订单查询场景：订单不存在
        String orderError = BusinessErrorCode.ORDER_NOT_FOUND;
        String orderMessage = BusinessErrorCode.getDefaultMessage(orderError);
        assertEquals("ORDER_NOT_FOUND", orderError);
        assertEquals("订单不存在", orderMessage);
    }

    @Test
    void testAllErrorCodesHaveMessages() {
        // 确保所有定义的错误码都有对应的默认消息
        String[] errorCodes = {
            BusinessErrorCode.USER_NOT_FOUND,
            BusinessErrorCode.USER_NOT_LOGIN,
            BusinessErrorCode.USER_ACCESS_DENIED,
            BusinessErrorCode.PRODUCT_NOT_FOUND,
            BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK,
            BusinessErrorCode.PRODUCT_OFFLINE,
            BusinessErrorCode.ORDER_NOT_FOUND,
            BusinessErrorCode.ORDER_STATUS_ERROR,
            BusinessErrorCode.ORDER_CANCELLED,
            BusinessErrorCode.PAYMENT_FAILED,
            BusinessErrorCode.PAYMENT_TIMEOUT,
            BusinessErrorCode.PAYMENT_INSUFFICIENT_BALANCE,
            BusinessErrorCode.SYSTEM_BUSY,
            BusinessErrorCode.SYSTEM_PARAM_ERROR,
            BusinessErrorCode.SYSTEM_ERROR
        };

        for (String errorCode : errorCodes) {
            String message = BusinessErrorCode.getDefaultMessage(errorCode);
            assertNotNull(message, "错误码 " + errorCode + " 没有默认消息");
            assertNotEquals("未知错误", message, "错误码 " + errorCode + " 使用了默认的未知错误消息");
        }
    }
}