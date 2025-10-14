package com.xpcjsu.sunshinemall.framework.convention.exception;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler测试类
 * <p>
 * 测试全局异常处理器的核心功能。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@ContextConfiguration(classes = {GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHandleValidationException() throws Exception {
        mockMvc.perform(get("/test/validation-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.SYSTEM_PARAM_ERROR))
                .andExpect(jsonPath("$.message").value("参数不能为空"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHandleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.PRODUCT_NOT_FOUND))
                .andExpect(jsonPath("$.message").value("商品不存在"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHandleSystemException() throws Exception {
        mockMvc.perform(get("/test/system-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.SYSTEM_ERROR))
                .andExpect(jsonPath("$.message").value("数据库连接失败"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHandleMethodArgumentNotValidException() throws Exception {
        String requestBody = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/validated-bean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.SYSTEM_PARAM_ERROR))
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch")
                        .param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.SYSTEM_PARAM_ERROR))
                .andExpect(jsonPath("$.message").value(containsString("参数'id'类型错误")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHandleUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.SYSTEM_ERROR))
                .andExpect(jsonPath("$.message").value(BusinessErrorCode.getDefaultMessage(BusinessErrorCode.SYSTEM_ERROR)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testExceptionWithContext() throws Exception {
        mockMvc.perform(get("/test/exception-with-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.ORDER_NOT_FOUND))
                .andExpect(jsonPath("$.message").value("订单不存在"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试用的Controller
     */
    @RestController
    @RequestMapping("/test")
    @Validated
    static class TestController {

        @GetMapping("/validation-exception")
        public Result<Void> throwValidationException() {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数不能为空");
        }

        @GetMapping("/business-exception")
        public Result<Void> throwBusinessException() {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_FOUND, "商品不存在");
        }

        @GetMapping("/system-exception")
        public Result<Void> throwSystemException() {
            throw new SystemException(BusinessErrorCode.SYSTEM_ERROR, "数据库连接失败");
        }

        @PostMapping("/validated-bean")
        public Result<Void> validateBean(@RequestBody @org.springframework.validation.annotation.Validated TestBean bean) {
            return Result.success();
        }

        @GetMapping("/type-mismatch")
        public Result<Void> typeMismatch(@RequestParam Integer id) {
            return Result.success();
        }

        @GetMapping("/unexpected-exception")
        public Result<Void> throwUnexpectedException() {
            throw new RuntimeException("未预期的异常");
        }

        @GetMapping("/exception-with-context")
        public Result<Void> throwExceptionWithContext() {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "订单不存在")
                    .addContext("orderId", "ORD20231014001")
                    .addContext("userId", "USER123");
        }
    }

    /**
     * 测试用的Bean
     */
    static class TestBean {
        @NotBlank(message = "name不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
