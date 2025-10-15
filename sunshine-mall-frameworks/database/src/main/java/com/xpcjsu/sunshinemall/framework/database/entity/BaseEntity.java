package com.xpcjsu.sunshinemall.framework.database.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * <p>
 * 所有数据库实体的基类，提供统一的基础字段和审计功能。
 * <p>
 * 字段说明：
 * <ul>
 * <li>id：主键，使用MyBatis-Plus内置雪花算法自动生成</li>
 * <li>createTime：创建时间，插入时自动填充</li>
 * <li>updateTime：更新时间，插入和更新时自动填充</li>
 * <li>delFlag：逻辑删除标识，0-未删除，1-已删除</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */

//Serializable标志类可以被序列化
@Data
public abstract class BaseEntity implements Serializable {

    /*Java序列化机制使用的唯一标识符
     验证序列化对象和反序列化对象的版本兼容性
     当类结构发生变化时，可通过此字段控制序列化兼容性
     值为1L表示这是该类的第一个版本*/
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     * <p>
     * 使用雪花算法（Snowflake）生成分布式唯一ID
     * IdType.ASSIGN_ID = 雪花算法（MyBatis-Plus默认实现）

     * ID类型	        说明
     * IdType.ASSIGN_ID	雪花算法（默认使用雪花算法生成Long类型ID）
     * IdType.AUTO	    数据库自增ID
     * IdType.INPUT	    手动输入
     * IdType.ASSIGN_UUID	UUID字符串
     * IdType.NONE	    无状态
     *
     * <p>
     * 后续可替换为自定义雪花算法实现（DistributedID模块）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     * <p>
     * 插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>
     * 插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     * <p>
     * 0-未删除，1-已删除
     */
    @TableLogic
    private Integer delFlag;
}
