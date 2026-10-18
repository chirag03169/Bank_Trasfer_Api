package com.example.payment_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void savePayment(Long paymentId, String status) {

        String redisKey = "payment:" + paymentId;

        redisTemplate.opsForHash().put(
                redisKey,
                "paymentId",
                paymentId.toString()
        );

        redisTemplate.opsForHash().put(
                redisKey,
                "status",
                status
        );

        redisTemplate.expire(redisKey, Duration.ofHours(24));
    }
}
