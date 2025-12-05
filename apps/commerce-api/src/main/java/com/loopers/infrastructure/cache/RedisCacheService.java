package com.loopers.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class RedisCacheService {
    private final RedisTemplate<String, String> redisTemplate;

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setValue(String key, String value, long timeoutInSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
    }

    public void deleteByKey(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        Set<String> keysToDelete = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keysToDelete::add);
        }
        if (!keysToDelete.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
                for (String key : keysToDelete) {
                    connection.commands().del(key.getBytes());
                }
                return null;
            });
        }
    }
}
