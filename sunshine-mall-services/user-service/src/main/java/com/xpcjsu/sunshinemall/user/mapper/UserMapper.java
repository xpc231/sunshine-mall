package com.xpcjsu.sunshinemall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
