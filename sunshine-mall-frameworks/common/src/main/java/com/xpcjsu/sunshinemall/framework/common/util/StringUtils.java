package com.xpcjsu.sunshinemall.framework.common.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 专注于敏感信息脱敏功能，线程安全的静态工具方法。
 * 支持手机号、身份证、邮箱等常见敏感信息的脱敏处理。
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class StringUtils {

    /**
     * 默认脱敏字符
     */
    private static final char DEFAULT_MASK_CHAR = '*';

    /**
     * 手机号正则表达式
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 身份证号正则表达式（18位）
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");



    /**
     * 私有构造器，防止实例化
     */
    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 检查字符串是否为空或null
     * 
     * @param str 待检查的字符串
     * @return 如果字符串为null或空字符串返回true，否则返回false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 检查字符串是否为空白（null、空字符串或只包含空白字符）
     * 
     * @param str 待检查的字符串
     * @return 如果字符串为null、空字符串或只包含空白字符返回true，否则返回false
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * 检查字符串是否不为空且不为空白
     * 
     * @param str 待检查的字符串
     * @return 如果字符串不为null且包含非空白字符返回true，否则返回false
     */
    public static boolean hasText(String str) {
        return !isBlank(str);
    }

    /**
     * 手机号脱敏
     * <p>
     * 格式：保留前3位和后4位，中间用*号替代
     * 示例：13812345678 -> 138****5678
     * 
     * @param mobile 手机号
     * @return 脱敏后的手机号
     * @throws ValidationException 如果手机号格式不正确
     */
    public static String maskMobile(String mobile) {
        if (isEmpty(mobile)) {
            return mobile;
        }

        if (!MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new ValidationException("INVALID_MOBILE", "手机号格式不正确")
                .addContext("mobile", mobile);
        }

        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 身份证号脱敏
     * <p>
     * 格式：保留前6位和后4位，中间用*号替代
     * 示例：110101199001011234 -> 110101********1234
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     * @throws ValidationException 如果身份证号格式不正确
     */
    public static String maskIdCard(String idCard) {
        if (isEmpty(idCard)) {
            return idCard;
        }

        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            throw new ValidationException("INVALID_ID_CARD", "身份证号格式不正确")
                .addContext("idCard", idCard);
        }

        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 邮箱脱敏
     * <p>
     * 格式：用户名保留前2位和@符号后的域名，中间用*号替代
     * 示例：username@example.com -> us****@example.com
     * 
     * @param email 邮箱地址
     * @return 脱敏后的邮箱地址
     * @throws ValidationException 如果邮箱格式不正确
     */
    public static String maskEmail(String email) {
        if (isEmpty(email)) {
            return email;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("INVALID_EMAIL", "邮箱格式不正确")
                .addContext("email", email);
        }

        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return "*".repeat(username.length()) + domain;
        }

        return username.substring(0, 2) + "*".repeat(4) + domain;
    }

    /**
     * 通用脱敏方法
     * <p>
     * 保留指定前缀和后缀长度的字符，中间用指定字符替代
     * 
     * @param str        原始字符串
     * @param prefixLen  保留前缀长度
     * @param suffixLen  保留后缀长度
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     * @throws ValidationException 如果参数不正确
     */
    public static String mask(String str, int prefixLen, int suffixLen, char maskChar) {
        if (isEmpty(str)) {
            return str;
        }

        if (prefixLen < 0 || suffixLen < 0) {
            throw new ValidationException("INVALID_MASK_PARAM", "脱敏参数不能为负数")
                .addContext("prefixLen", prefixLen)
                .addContext("suffixLen", suffixLen);
        }

        int len = str.length();
        if (prefixLen + suffixLen >= len) {
            return String.valueOf(maskChar).repeat(len);
        }

        return str.substring(0, prefixLen) + 
               String.valueOf(maskChar).repeat(len - prefixLen - suffixLen) + 
               str.substring(len - suffixLen);
    }

    /**
     * 通用脱敏方法（使用默认脱敏字符*）
     * 
     * @param str       原始字符串
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixLen, int suffixLen) {
        return mask(str, prefixLen, suffixLen, DEFAULT_MASK_CHAR);
    }


}