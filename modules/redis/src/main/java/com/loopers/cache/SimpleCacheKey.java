package com.loopers.cache;

import java.time.Duration;

public record SimpleCacheKey<T>(String key, Duration ttl, Class<T> type) implements CacheKey<T> {

  public static <T> SimpleCacheKey<T> of(String key, Duration ttl, Class<T> clazz) {
    return new SimpleCacheKey<>(key, ttl, clazz);
  }
}
