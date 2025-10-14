package com.xpcjsu.sunshinemall.framework.convention.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result统一响应结果测试类
 * <p>
 * 专注于测试电商业务API响应的核心功能。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class ResultTest {

    @Test
    void testSuccessWithoutData() {
        Result<String> result = Result.success();
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals(Result.SUCCESS_MESSAGE, result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void testSuccessWithData() {
        String testData = "测试数据";
        Result<String> result = Result.success(testData);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals(Result.SUCCESS_MESSAGE, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSuccessWithDataAndMessage() {
        String testData = "订单数据";
        String customMessage = "订单查询成功";
        Result<String> result = Result.success(testData, customMessage);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals(customMessage, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testFailure() {
        String errorCode = "ORDER_NOT_FOUND";
        String errorMessage = "订单不存在";
        Result<String> result = Result.failure(errorCode, errorMessage);
        
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(errorCode, result.getCode());
        assertEquals(errorMessage, result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testGenericTypes() {
        // 测试整数类型
        Result<Integer> intResult = Result.success(123);
        assertEquals(Integer.valueOf(123), intResult.getData());
        
        // 测试对象类型
        TestData testData = new TestData("test", 456);
        Result<TestData> objectResult = Result.success(testData);
        assertEquals(testData, objectResult.getData());
        assertEquals("test", objectResult.getData().getName());
        assertEquals(456, objectResult.getData().getValue());
    }

    @Test
    void testConstants() {
        assertEquals("0", Result.SUCCESS_CODE);
        assertEquals("操作成功", Result.SUCCESS_MESSAGE);
    }

    @Test
    void testSettersAndGetters() {
        Result<String> result = Result.success();
        
        // 测试设置和获取
        result.setCode("CUSTOM_CODE");
        assertEquals("CUSTOM_CODE", result.getCode());
        
        result.setMessage("自定义消息");
        assertEquals("自定义消息", result.getMessage());
        
        result.setData("自定义数据");
        assertEquals("自定义数据", result.getData());
        
        Long customTimestamp = 1697203200000L;
        result.setTimestamp(customTimestamp);
        assertEquals(customTimestamp, result.getTimestamp());
    }

    @Test
    void testToString() {
        String testData = "测试";
        Result<String> result = Result.success(testData, "成功");
        
        String toString = result.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("code='0'"));
        assertTrue(toString.contains("message='成功'"));
        assertTrue(toString.contains("data=测试"));
        assertTrue(toString.contains("timestamp="));
    }

    @Test
    void testTimestampAccuracy() throws InterruptedException {
        Result<String> result1 = Result.success();
        Thread.sleep(1); // 确保时间差
        Result<String> result2 = Result.success();
        
        // 时间戳应该不同且递增
        assertTrue(result2.getTimestamp() >= result1.getTimestamp());
    }

    @Test
    void testSerializable() {
        // 验证Result实现了Serializable接口
        assertTrue(java.io.Serializable.class.isAssignableFrom(Result.class));
    }

    @Test
    void testBusinessScenarios() {
        // 电商业务场景测试
        
        // 商品查询成功
        Result<String> productResult = Result.success("商品信息", "商品查询成功");
        assertTrue(productResult.isSuccess());
        assertEquals("商品信息", productResult.getData());
        
        // 库存不足
        Result<Void> stockResult = Result.failure("INSUFFICIENT_STOCK", "商品库存不足");
        assertTrue(stockResult.isFailure());
        assertEquals("INSUFFICIENT_STOCK", stockResult.getCode());
        
        // 订单创建成功
        String orderId = "ORD20231013001";
        Result<String> orderResult = Result.success(orderId, "订单创建成功");
        assertTrue(orderResult.isSuccess());
        assertEquals(orderId, orderResult.getData());
    }

    /**
     * 测试用的数据类
     */
    private static class TestData {
        private String name;
        private int value;

        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestData testData = (TestData) obj;
            return value == testData.value && name.equals(testData.name);
        }
    }
}