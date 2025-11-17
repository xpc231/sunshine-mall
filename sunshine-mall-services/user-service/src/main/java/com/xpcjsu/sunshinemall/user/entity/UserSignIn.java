package com.xpcjsu.sunshinemall.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_sign_in")
public class UserSignIn {
    @TableId
    private Long id;
    private Long userId;
    private LocalDate signDate;
    private Integer continueDays;
    private Integer rewardPoints;
    private LocalDateTime createTime;
}