package com.xpcjsu.sunshinemall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserAddressDTO {
    private Long id;
    @NotNull
    private Long userId;
    @NotBlank
    private String name;
    @NotBlank
    private String phone;
    @NotBlank
    private String province;
    @NotBlank
    private String city;
    @NotBlank
    private String district;
    @NotBlank
    private String address;
    private Integer isDefault;
}