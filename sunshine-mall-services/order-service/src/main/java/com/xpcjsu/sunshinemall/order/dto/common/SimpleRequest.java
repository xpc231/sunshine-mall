package com.xpcjsu.sunshinemall.order.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用简单请求DTO
 * 用于只需要一个字符串参数的场景
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SimpleRequest", description = "简单请求")
public class SimpleRequest {

    @NotBlank(message = "参数不能为空")
    @Schema(description = "请求参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;
}