package com.xpcjsu.sunshinemall.framework.convention.errorcode;

/**
 * 电商业务错误码
 * <p>
 * 专注于电商核心业务场景的错误码定义，避免过度设计。
 * 采用模块化设计，便于扩展和维护。
 * <p>
 * 错误码格式：模块_场景_错误类型
 * <ul>
 * <li>USER_*：用户相关错误</li>
 * <li>PRODUCT_*：商品相关错误</li>
 * <li>ORDER_*：订单相关错误</li>
 * <li>PAYMENT_*：支付相关错误</li>
 * <li>SYSTEM_*：系统相关错误</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class BusinessErrorCode {

    /**
     * 私有构造器，防止实例化
     */
    private BusinessErrorCode() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 用户相关错误码 ==========

    /**
     * 用户不存在
     */
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    /**
     * 用户未登录
     */
    public static final String USER_NOT_LOGIN = "USER_NOT_LOGIN";

    /**
     * 用户权限不足
     */
    public static final String USER_ACCESS_DENIED = "USER_ACCESS_DENIED";

    // ========== 商品相关错误码 ==========

    /**
     * 商品不存在
     */
    public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

    /**
     * 商品库存不足
     */
    public static final String PRODUCT_INSUFFICIENT_STOCK = "PRODUCT_INSUFFICIENT_STOCK";

    /**
     * 商品已下架
     */
    public static final String PRODUCT_OFFLINE = "PRODUCT_OFFLINE";

    // ========== 订单相关错误码 ==========

    /**
     * 订单不存在
     */
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    /**
     * 订单状态错误
     */
    public static final String ORDER_STATUS_ERROR = "ORDER_STATUS_ERROR";

    /**
     * 订单已取消
     */
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    // ========== 支付相关错误码 ==========

    /**
     * 支付失败
     */
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    /**
     * 支付超时
     */
    public static final String PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";

    /**
     * 余额不足
     */
    public static final String PAYMENT_INSUFFICIENT_BALANCE = "PAYMENT_INSUFFICIENT_BALANCE";

    // ========== 系统相关错误码 ==========

    /**
     * 系统繁忙
     */
    public static final String SYSTEM_BUSY = "SYSTEM_BUSY";

    /**
     * 参数错误
     */
    public static final String SYSTEM_PARAM_ERROR = "SYSTEM_PARAM_ERROR";

    /**
     * 系统异常
     */
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    /**
     * 获取错误码对应的默认消息
     * <p>
     * 提供常用错误码的默认消息，减少重复定义
     * 
     * @param errorCode 错误码
     * @return 默认错误消息
     */
    public static String getDefaultMessage(String errorCode) {
        if (errorCode == null) {
            return "未知错误";
        }

        switch (errorCode) {
            // 用户相关
            case USER_NOT_FOUND:
                return "用户不存在";
            case USER_NOT_LOGIN:
                return "用户未登录";
            case USER_ACCESS_DENIED:
                return "用户权限不足";

            // 商品相关
            case PRODUCT_NOT_FOUND:
                return "商品不存在";
            case PRODUCT_INSUFFICIENT_STOCK:
                return "商品库存不足";
            case PRODUCT_OFFLINE:
                return "商品已下架";

            // 订单相关
            case ORDER_NOT_FOUND:
                return "订单不存在";
            case ORDER_STATUS_ERROR:
                return "订单状态错误";
            case ORDER_CANCELLED:
                return "订单已取消";

            // 支付相关
            case PAYMENT_FAILED:
                return "支付失败";
            case PAYMENT_TIMEOUT:
                return "支付超时";
            case PAYMENT_INSUFFICIENT_BALANCE:
                return "余额不足";

            // 系统相关
            case SYSTEM_BUSY:
                return "系统繁忙，请稍后重试";
            case SYSTEM_PARAM_ERROR:
                return "参数错误";
            case SYSTEM_ERROR:
                return "系统异常";

            default:
                return "未知错误";
        }
    }
}