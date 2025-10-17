package com.xpcjsu.sunshinemall.framework.distributedid.example;

import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;

/**
 * 分布式ID生成器使用示例
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class IdGeneratorExample {

    public static void main(String[] args) {
        // 设置配置（实际项目中通过配置文件设置）
        System.setProperty("distributedid.datacenter-id", "1");
        System.setProperty("distributedid.worker-id", "1");

        // 方式1：通过SingletonHolder获取单例
        SnowflakeIdGenerator generator = SingletonHolder.getInstance(SnowflakeIdGenerator.class);

        System.out.println("========== 生成ID示例 ==========");
        // 生成10个ID
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            System.out.println("生成ID [" + (i + 1) + "]: " + id);
        }

        System.out.println("\n========== ID解析示例 ==========");
        long sampleId = generator.nextId();
        String parsedInfo = generator.parseId(sampleId);
        System.out.println(parsedInfo);

        System.out.println("\n========== 配置信息 ==========");
        System.out.println("数据中心ID: " + generator.getDatacenterId());
        System.out.println("工作机器ID: " + generator.getWorkerId());

        System.out.println("\n========== 性能测试 ==========");
        int count = 100000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.printf("生成 %d 个ID耗时: %d ms%n", count, duration);
        System.out.printf("平均耗时: %.2f μs/ID%n", (duration * 1000.0) / count);
        System.out.printf("QPS: %.0f%n", (count * 1000.0) / duration);
    }
}
