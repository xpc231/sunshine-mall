package com.xpcjsu.sunshinemall.framework.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 字段自动填充处理器
 * <p>
 * 自动填充BaseEntity的创建时间和更新时间字段。
 * <p>
 * 填充规则：
 * <ul>
 * <li>插入时：自动填充createTime和updateTime</li>
 * <li>更新时：自动填充updateTime</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */

//MyBatis-Plus在执行数据库操作时，会自动检测并调用已注册的MetaObjectHandler
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        //MyBatis反射元对象,需要填充的字段名称,字段类型,字段值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        log.debug("插入操作 - 自动填充时间字段: {}", now);
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);

        log.debug("更新操作 - 自动填充更新时间: {}", now);
    }
}
