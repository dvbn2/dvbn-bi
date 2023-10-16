package com.dvbn.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author dvbn
 * @title: RedisLimiterManagerTest
 * @createDate 2023/10/15 21:07
 */
@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimit() {
        String key = "1";
        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doRateLimit(key);
            System.out.println("成功");
        }
    }
}