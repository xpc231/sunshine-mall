package com.xpcjsu.sunshinemall.user.service.impl;

import com.xpcjsu.sunshinemall.user.service.UserSignInService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class UserSignInServiceImpl implements UserSignInService {

    private final StringRedisTemplate stringRedisTemplate;

    public UserSignInServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 签到
    @Override
    public boolean signToday(Long userId) {
        // 获取当前时间
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.from(now);
        // 构建 key
        String key = buildKey(userId, ym);
        // 获取偏移量
        int offset = now.getDayOfMonth() - 1;
        // 判断是否已签到
        Boolean signed = stringRedisTemplate.opsForValue().getBit(key, offset);
        if (Boolean.TRUE.equals(signed)) {
            return false;
        }
        // 签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return true;
    }

    // 查询签到状态
    @Override
    public boolean isSignedToday(Long userId) {

        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.from(now);

        String key = buildKey(userId, ym);

        int offset = now.getDayOfMonth() - 1;

        Boolean signed = stringRedisTemplate.opsForValue().getBit(key, offset);

        return Boolean.TRUE.equals(signed);
    }

    // 查询签到次数
    @Override
    public int countMonth(Long userId, YearMonth month) {

        String key = buildKey(userId, month);

        //将字符串类型的Redis键序列化为字节数组
        byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(key);

        /**
         * BitCount指令，计数操作，统计指定键的位图中设置为1的位数量
         *
         * @param connection Redis连接对象，用于执行底层Redis命令
         * @return 指定键的位图中位值为1的总数量
         */
        Long count = stringRedisTemplate.execute((RedisConnection connection) -> connection.bitCount(rawKey));
        return count == null ? 0 : count.intValue();
    }

    // 构建 key
    private String buildKey(Long userId, YearMonth month) {
        String ym = month.format(DateTimeFormatter.ofPattern("yyyyMM"));
        return "sign:" + userId + ":" + ym;
    }
}