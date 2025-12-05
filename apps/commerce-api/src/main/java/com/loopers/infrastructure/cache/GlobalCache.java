package com.loopers.infrastructure.cache;

public interface GlobalCache {
    String get(String key);

    void set(String key, String value, long timeoutInSeconds);

    void delete(String key);

    void deleteByPattern(String pattern);
}
