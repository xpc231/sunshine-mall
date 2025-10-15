package com.xpcjsu.sunshinemall.framework.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.framework.database.entity.TestUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试Mapper
 */
@Mapper
public interface TestUserMapper extends BaseMapper<TestUser> {
}
