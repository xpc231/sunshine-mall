package com.xpcjsu.sunshinemall.framework.database.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * <p>
 * 配置内容：
 * <ul>
 * <li>分页插件：支持MySQL分页查询</li>
 * <li>防全表更新删除插件：防止误操作</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        //DbType.MYSQL参数适配MySQL数据库
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L); // 单页最大500条
        //将分页拦截器添加到MyBatis-Plus拦截器链中
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 安全防护插件,防全表更新删除插件
        /* 拦截没有WHERE条件的UPDATE和DELETE语句
           当检测到危险操作时自动阻断并抛出异常
           保护数据库免受意外的全表数据修改*/
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        return interceptor;
    }
}
