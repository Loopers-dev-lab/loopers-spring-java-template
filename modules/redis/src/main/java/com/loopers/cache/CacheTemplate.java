package com.loopers.cache;

import java.util.Optional;
import java.util.function.Supplier;

public interface CacheTemplate {

  <T> Optional<T> get(CacheKey<T> cacheKey);

  <T> void put(CacheKey<T> cacheKey, T value);

  //캐시 무효화 필요 시 사용
  void evict(CacheKey<?> cacheKey);

  <T> T getOrLoad(CacheKey<T> cacheKey, Supplier<T> loader);
}
