package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping("/redis/test")
    public String testRedis() {
        try {
            // Test connection first
            redisTemplate.getConnectionFactory().getConnection().ping();
            
            // Test write
            redisTemplate.opsForValue().set("test-key", "Hello from Redis Cloud!");
            
            // Test read
            String value = redisTemplate.opsForValue().get("test-key");
            
            return "Redis is working! Stored and retrieved: " + value;
        } catch (Exception e) {
            return "Redis connection failed: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}