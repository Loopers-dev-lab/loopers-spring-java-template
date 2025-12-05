package com.loopers.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class GlobalCacheImpl implements GlobalCache {
    private final RedisCacheService redisCacheService;

    @Override
    public String get(String key) {
        return redisCacheService.getValue(key);
    }

    @Override
    public void set(String key, String value, long timeoutInSeconds) {
        redisCacheService.setValue(key, value, timeoutInSeconds);
    }

    @Override
    public void delete(String key) {
        redisCacheService.deleteByKey(key);
    }

    @Override
    public void deleteByPattern(String pattern) {
        redisCacheService.deleteByPattern(pattern);
    }
}
