package com.dvbn.springbootinit.manager;

import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author dvbn
 * @title: 提供RedisLimiter限流基础服务
 * @createDate 2023/10/15 20:42
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;


    /**
     * 限流操作
     *
     * @param key 区分不同的限流对象的唯一标识
     */
    public void doRateLimit(String key) {
        // 获取RateLimiter实例
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 每个key每1秒限制两个令牌
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        // 当一个操作来了之后就会请求一个令牌
        boolean b = rateLimiter.tryAcquire(1);// 每个key限制一次，可用于普通用户和会员用户访问功能的次数

        if (!b) {
            // 未获取到令牌
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}

