package com.xpcjsu.sunshinemall.framework.common.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringUtils测试类
 * <p>
 * 专注于测试敏感信息脱敏功能。
 * 其他字符串操作建议使用成熟的工具库：
 * <ul>
 * <li>基础字符串操作：Spring Framework 的 StringUtils</li>
 * <li>高级字符串操作：Hutool 的 StrUtil</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class StringUtilsTest {

    @Test
    void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty("hello"));
    }

    @Test
    void testHasText() {
        assertFalse(StringUtils.hasText(null));
        assertFalse(StringUtils.hasText(""));
        assertFalse(StringUtils.hasText(" "));
        assertFalse(StringUtils.hasText("  \t\n  "));
        assertTrue(StringUtils.hasText("hello"));
        assertTrue(StringUtils.hasText(" hello "));
    }

    @Test
    void testMaskMobile() {
        // 正常手机号
        assertEquals("138****5678", StringUtils.maskMobile("13812345678"));
        assertEquals("159****9999", StringUtils.maskMobile("15999999999"));

        // 空字符串和null
        assertNull(StringUtils.maskMobile(null));
        assertEquals("", StringUtils.maskMobile(""));

        // 无效手机号
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            StringUtils.maskMobile("1234567890"));
        assertEquals("INVALID_MOBILE", exception.getErrorCode());
        assertEquals("1234567890", exception.getContext().get("mobile"));

        // 长度不正确
        assertThrows(ValidationException.class, () -> 
            StringUtils.maskMobile("1381234567"));
    }

    @Test
    void testMaskIdCard() {
        // 正常身份证号
        assertEquals("110101********1234", StringUtils.maskIdCard("110101199001011234"));
        assertEquals("330106********5674", StringUtils.maskIdCard("330106198506125674"));

        // 空字符串和null
        assertNull(StringUtils.maskIdCard(null));
        assertEquals("", StringUtils.maskIdCard(""));

        // 无效身份证号
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            StringUtils.maskIdCard("12345678901234567"));
        assertEquals("INVALID_ID_CARD", exception.getErrorCode());
    }

    @Test
    void testMaskEmail() {
        // 正常邮箱
        assertEquals("us****@example.com", StringUtils.maskEmail("username@example.com"));
        assertEquals("ab****@test.org", StringUtils.maskEmail("abcdef@test.org"));
        
        // 短用户名
        assertEquals("**@test.com", StringUtils.maskEmail("ab@test.com"));

        // 空字符串和null
        assertNull(StringUtils.maskEmail(null));
        assertEquals("", StringUtils.maskEmail(""));

        // 无效邮箱
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            StringUtils.maskEmail("invalid-email"));
        assertEquals("INVALID_EMAIL", exception.getErrorCode());
    }

    @Test
    void testMaskGeneric() {
        // 正常情况
        assertEquals("ab***fg", StringUtils.mask("abcdefg", 2, 2));
        assertEquals("12####78", StringUtils.mask("12345678", 2, 2, '#'));

        // 边界情况
        assertEquals("****", StringUtils.mask("test", 2, 2));
        assertEquals("****", StringUtils.mask("test", 5, 5));

        // 空字符串和null
        assertNull(StringUtils.mask(null, 1, 1));
        assertEquals("", StringUtils.mask("", 1, 1));

        // 无效参数
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            StringUtils.mask("test", -1, 1));
        assertEquals("INVALID_MASK_PARAM", exception.getErrorCode());
    }

    @Test
    void testUtilityClassCannotBeInstantiated() {
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Constructor<StringUtils> constructor = StringUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}