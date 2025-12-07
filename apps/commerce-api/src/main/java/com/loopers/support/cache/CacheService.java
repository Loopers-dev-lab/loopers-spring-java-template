package com.loopers.support.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * ------------------------------
     * 1) Class<T> 기반 역직렬화 (단일 객체)
     * ------------------------------
     */
    public <T> T getOrLoad(
            String key,
            Supplier<T> loader,
            Duration ttl,
            Class<T> clazz
    ) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, clazz);
            }
        } catch (Exception e) {
            log.warn("캐시 역직렬화 실패, key: {}, error: {}", key, e.getMessage());
        }

        return loadAndCache(key, loader, ttl);
    }

    /**
     * ------------------------------
     * 2) TypeReference<T> 기반 역직렬화 (제네릭 구조)
     * ------------------------------
     */
    public <T> T getOrLoad(
            String key,
            Supplier<T> loader,
            Duration ttl,
            TypeReference<T> typeReference
    ) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, typeReference);
            }
        } catch (Exception e) {
            log.warn("캐시 역직렬화 실패, key: {}, error: {}", key, e.getMessage());
        }

        return loadAndCache(key, loader, ttl);
    }


    public <T> T loadAndCache(String key, Supplier<T> loader, Duration ttl) {
        T actualData = loader.get();
        if (actualData != null) {
            save(key, actualData, ttl);
        }
        return actualData;
    }

    public <T> void save(String key, T value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
        } catch (Exception e) {
            log.warn("캐시 저장 실패, key: {}, error: {}", key, e.getMessage());
        }
    }
}
