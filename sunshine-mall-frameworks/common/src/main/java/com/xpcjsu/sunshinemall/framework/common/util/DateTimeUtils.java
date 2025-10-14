package com.xpcjsu.sunshinemall.framework.common.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * <p>
 * 专注于电商业务中的核心时间处理功能，线程安全的静态工具方法。
 * 提供业务常用的时间戳、格式化和时间范围判断等功能。
 * <p>
 * 注意：其他时间操作建议使用成熟的工具库：
 * <ul>
 * <li>基础时间操作：Java 8+ 的 java.time API</li>
 * <li>高级时间操作：Hutool 的 DateUtil</li>
 * <li>时区处理：Spring 的 TimeZone 支持</li>
 * <li>复杂计算：Apache Commons Lang 的 DateUtils</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class DateTimeUtils {

    /**
     * 电商业务时间戳格式（用于订单号、流水号等）
     */
    public static final String BUSINESS_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    /**
     * 预编译的业务时间戳格式化器（线程安全）
     */
    private static final DateTimeFormatter BUSINESS_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(BUSINESS_TIMESTAMP_PATTERN);

    /**
     * 私有构造器，防止实例化
     */
    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 当前时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }


    /**
     * 获取业务时间戳字符串
     * <p>
     * 格式：yyyyMMddHHmmss，用于订单号、流水号等业务场景
     * 
     * @return 业务时间戳字符串
     */
    public static String getBusinessTimestamp() {
        return LocalDateTime.now().format(BUSINESS_TIMESTAMP_FORMATTER);
    }


    /**
     * 判断日期是否在业务时间范围内
     * <p>
     * 电商业务常用场景：活动时间、促销时间、订单有效期等
     * 
     * @param date      待检查的日期
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 如果在范围内返回true，否则返回false
     * @throws ValidationException 如果参数为null
     */
    public static boolean isInBusinessRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null) {
            throw new ValidationException("NULL_DATE", "待检查日期不能为null");
        }
        if (startDate == null) {
            throw new ValidationException("NULL_START_DATE", "开始日期不能为null");
        }
        if (endDate == null) {
            throw new ValidationException("NULL_END_DATE", "结束日期不能为null");
        }

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }


}