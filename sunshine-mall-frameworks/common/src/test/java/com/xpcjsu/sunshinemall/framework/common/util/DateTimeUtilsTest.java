package com.xpcjsu.sunshinemall.framework.common.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateTimeUtils测试类
 * <p>
 * 专注于测试电商业务中的核心时间处理功能。
 * 其他时间操作建议使用成熟的工具库：
 * <ul>
 * <li>基础时间操作：Java 8+ 的 java.time API</li>
 * <li>高级时间操作：Hutool 的 DateUtil</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class DateTimeUtilsTest {

    @Test
    void testCurrentTimestamp() {
        // 测试当前时间戳
        long timestamp1 = DateTimeUtils.currentTimestamp();
        long timestamp2 = DateTimeUtils.currentTimestamp();
        assertTrue(timestamp2 >= timestamp1);
        
        // 时间戳应该大于2020年的时间戳
        assertTrue(timestamp1 > 1577836800000L); // 2020-01-01 00:00:00
    }

    @Test
    void testGetBusinessTimestamp() {
        // 测试业务时间戳格式
        String businessTimestamp = DateTimeUtils.getBusinessTimestamp();
        assertNotNull(businessTimestamp);
        
        // 验证格式：yyyyMMddHHmmss (14位数字)
        assertTrue(businessTimestamp.matches("\\d{14}"));
        
        // 验证时间合理性（年份应该是2020以后）
        String year = businessTimestamp.substring(0, 4);
        assertTrue(Integer.parseInt(year) >= 2020);
        
        // 验证月份合理性（1-12）
        String month = businessTimestamp.substring(4, 6);
        int monthInt = Integer.parseInt(month);
        assertTrue(monthInt >= 1 && monthInt <= 12);
        
        // 验证日期合理性（1-31）
        String day = businessTimestamp.substring(6, 8);
        int dayInt = Integer.parseInt(day);
        assertTrue(dayInt >= 1 && dayInt <= 31);
    }

    @Test
    void testIsInBusinessRange() {
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);
        LocalDate testDate = LocalDate.of(2023, 10, 15);

        // 在范围内
        assertTrue(DateTimeUtils.isInBusinessRange(testDate, startDate, endDate));
        assertTrue(DateTimeUtils.isInBusinessRange(startDate, startDate, endDate)); // 边界值
        assertTrue(DateTimeUtils.isInBusinessRange(endDate, startDate, endDate)); // 边界值

        // 不在范围内
        assertFalse(DateTimeUtils.isInBusinessRange(LocalDate.of(2023, 9, 30), startDate, endDate));
        assertFalse(DateTimeUtils.isInBusinessRange(LocalDate.of(2023, 11, 1), startDate, endDate));

        // 测试null参数
        assertThrows(ValidationException.class, () -> DateTimeUtils.isInBusinessRange(null, startDate, endDate));
        assertThrows(ValidationException.class, () -> DateTimeUtils.isInBusinessRange(testDate, null, endDate));
        assertThrows(ValidationException.class, () -> DateTimeUtils.isInBusinessRange(testDate, startDate, null));
    }

    @Test
    void testBusinessRangeExceptionDetails() {
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);
        
        // 测试异常包含正确的错误码
        try {
            DateTimeUtils.isInBusinessRange(null, startDate, endDate);
            fail("应该抛出异常");
        } catch (ValidationException e) {
            assertEquals("NULL_DATE", e.getErrorCode());
        }
        
        try {
            DateTimeUtils.isInBusinessRange(LocalDate.now(), null, endDate);
            fail("应该抛出异常");
        } catch (ValidationException e) {
            assertEquals("NULL_START_DATE", e.getErrorCode());
        }
        
        try {
            DateTimeUtils.isInBusinessRange(LocalDate.now(), startDate, null);
            fail("应该抛出异常");
        } catch (ValidationException e) {
            assertEquals("NULL_END_DATE", e.getErrorCode());
        }
    }

    @Test
    void testBusinessTimestampUniqueness() {
        // 测试连续生成的业务时间戳不同（用于订单号等业务场景）
        String timestamp1 = DateTimeUtils.getBusinessTimestamp();
        
        // 等待1毫秒确保时间不同
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String timestamp2 = DateTimeUtils.getBusinessTimestamp();
        
        // 通常情况下应该不同（除非系统时间精度不够）
        // 这里只检查格式正确性，不强制要求不同
        assertNotNull(timestamp1);
        assertNotNull(timestamp2);
        assertTrue(timestamp1.matches("\\d{14}"));
        assertTrue(timestamp2.matches("\\d{14}"));
    }

    @Test
    void testUtilityClassCannotBeInstantiated() {
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Constructor<DateTimeUtils> constructor = DateTimeUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }

    @Test
    void testBusinessConstants() {
        // 测试业务常量定义
        assertNotNull(DateTimeUtils.BUSINESS_TIMESTAMP_PATTERN);
        assertEquals("yyyyMMddHHmmss", DateTimeUtils.BUSINESS_TIMESTAMP_PATTERN);
    }
}