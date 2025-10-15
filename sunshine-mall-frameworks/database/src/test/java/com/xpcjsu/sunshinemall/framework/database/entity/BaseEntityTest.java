package com.xpcjsu.sunshinemall.framework.database.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.database.DatabaseTestApplication;
import com.xpcjsu.sunshinemall.framework.database.mapper.TestUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseEntity测试类
 */
@SpringBootTest(classes = DatabaseTestApplication.class)
@ActiveProfiles("test")
class BaseEntityTest {

    @Autowired
    private TestUserMapper testUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 创建测试表
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_user");
        jdbcTemplate.execute(
            "CREATE TABLE test_user (" +
            "id BIGINT PRIMARY KEY, " +
            "username VARCHAR(50), " +
            "email VARCHAR(100), " +
            "age INT, " +
            "create_time TIMESTAMP, " +
            "update_time TIMESTAMP, " +
            "del_flag INT DEFAULT 0" +
            ")"
        );
    }

    @Test
    void testInsertWithAutoFill() {
        // 创建测试用户
        TestUser user = new TestUser();
        user.setUsername("张三");
        user.setEmail("zhangsan@example.com");
        user.setAge(25);

        // 插入前时间字段应为null
        assertNull(user.getCreateTime());
        assertNull(user.getUpdateTime());

        // 执行插入
        int result = testUserMapper.insert(user);
        assertEquals(1, result);

        // 验证ID自动生成（雪花算法）
        assertNotNull(user.getId());
        assertTrue(user.getId() > 0);

        // 验证时间字段自动填充
        assertNotNull(user.getCreateTime());
        assertNotNull(user.getUpdateTime());
        assertEquals(user.getCreateTime(), user.getUpdateTime());
    }

    @Test
    void testUpdateWithAutoFill() throws InterruptedException {
        // 插入测试数据
        TestUser user = new TestUser();
        user.setUsername("李四");
        user.setEmail("lisi@example.com");
        user.setAge(30);
        testUserMapper.insert(user);

        LocalDateTime createTime = user.getCreateTime();
        LocalDateTime firstUpdateTime = user.getUpdateTime();

        // 等待1秒确保更新时间不同
        Thread.sleep(1000);

        // 更新数据
        user.setAge(31);
        testUserMapper.updateById(user);

        // 重新查询
        TestUser updated = testUserMapper.selectById(user.getId());

        // 验证创建时间未变
        assertEquals(createTime, updated.getCreateTime());

        // 验证更新时间已变
        assertNotNull(updated.getUpdateTime());
        assertTrue(updated.getUpdateTime().isAfter(firstUpdateTime));
    }

    @Test
    void testLogicDelete() {
        // 插入测试数据
        TestUser user = new TestUser();
        user.setUsername("王五");
        user.setEmail("wangwu@example.com");
        user.setAge(28);
        testUserMapper.insert(user);

        Long userId = user.getId();

        // 验证delFlag默认为0
        assertEquals(0, user.getDelFlag());

        // 执行逻辑删除
        int result = testUserMapper.deleteById(userId);
        assertEquals(1, result);

        // 验证查询不到已删除数据
        TestUser deleted = testUserMapper.selectById(userId);
        assertNull(deleted);

        // 直接查询数据库，验证数据仍存在且delFlag=1
        Integer delFlag = jdbcTemplate.queryForObject(
            "SELECT del_flag FROM test_user WHERE id = ?",
            Integer.class,
            userId
        );
        assertEquals(1, delFlag);
    }

    @Test
    void testPagination() {
        // 批量插入测试数据
        for (int i = 1; i <= 10; i++) {
            TestUser user = new TestUser();
            user.setUsername("用户" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // 分页查询：每页3条，查询第2页
        Page<TestUser> page = new Page<>(2, 3);
        Page<TestUser> result = testUserMapper.selectPage(page, new QueryWrapper<>());

        // 验证分页结果
        assertEquals(10, result.getTotal());
        assertEquals(4, result.getPages());
        assertEquals(3, result.getRecords().size());
        assertEquals("用户4", result.getRecords().get(0).getUsername());
    }

    @Test
    void testQueryWithCondition() {
        // 插入测试数据
        TestUser user1 = new TestUser();
        user1.setUsername("赵六");
        user1.setEmail("zhaoliu@example.com");
        user1.setAge(25);
        testUserMapper.insert(user1);

        TestUser user2 = new TestUser();
        user2.setUsername("孙七");
        user2.setEmail("sunqi@example.com");
        user2.setAge(35);
        testUserMapper.insert(user2);

        // 条件查询：年龄大于30
        QueryWrapper<TestUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.gt("age", 30);
        List<TestUser> users = testUserMapper.selectList(queryWrapper);

        // 验证查询结果
        assertEquals(1, users.size());
        assertEquals("孙七", users.get(0).getUsername());
        assertEquals(35, users.get(0).getAge());
    }

    @Test
    void testSnowflakeIdUniqueness() {
        // 插入多条数据，验证ID唯一性
        TestUser user1 = new TestUser();
        user1.setUsername("测试1");
        testUserMapper.insert(user1);

        TestUser user2 = new TestUser();
        user2.setUsername("测试2");
        testUserMapper.insert(user2);

        TestUser user3 = new TestUser();
        user3.setUsername("测试3");
        testUserMapper.insert(user3);

        // 验证ID不同
        assertNotEquals(user1.getId(), user2.getId());
        assertNotEquals(user2.getId(), user3.getId());
        assertNotEquals(user1.getId(), user3.getId());

        // 验证ID递增（雪花算法特性）
        assertTrue(user2.getId() > user1.getId());
        assertTrue(user3.getId() > user2.getId());
    }
}
