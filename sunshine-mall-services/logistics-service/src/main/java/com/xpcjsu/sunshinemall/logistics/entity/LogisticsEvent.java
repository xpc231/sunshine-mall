package com.xpcjsu.sunshinemall.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xpcjsu.sunshinemall.framework.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_event")
public class LogisticsEvent extends BaseEntity {
    private String shipmentNo;
    private String status;
    private LocalDateTime eventTime;
    private String location;
    private String message;
}