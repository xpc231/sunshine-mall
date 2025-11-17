package com.xpcjsu.sunshinemall.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xpcjsu.sunshinemall.framework.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_shipment")
public class LogisticsShipment extends BaseEntity {
    private String shipmentNo;
    private String orderNo;
    private String carrierCode;
    private String carrierName;
    private String status;
    private String trackingCode;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String senderAddress;
    private String remark;
}