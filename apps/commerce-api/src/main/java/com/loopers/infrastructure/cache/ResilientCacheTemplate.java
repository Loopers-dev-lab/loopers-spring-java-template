package com.loopers.infrastructure.cache;

import com.loopers.cache.CacheKey;
import com.loopers.cache.CacheTemplate;
import com.loopers.cache.RedisCacheTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@Profile("!test")
@RequiredArgsConstructor
public class ResilientCacheTemplate implements CacheTemplate {

  private final RedisCacheTemplate delegate;

  @Override
  @CircuitBreaker(name = "redis-cache", fallbackMethod = "getFallback")
  public <T> Optional<T> get(CacheKey<T> cacheKey) {
    return delegate.get(cacheKey);
  }

  private <T> Optional<T> getFallback(CacheKey<T> cacheKey, Throwable t) {
    log.warn("Redis 서킷 Open, get fallback: key={}, error={}", cacheKey.key(), t.getMessage());
    return Optional.empty();
  }

  @Override
  @CircuitBreaker(name = "redis-cache", fallbackMethod = "putFallback")
  public <T> void put(CacheKey<T> cacheKey, T value) {
    delegate.put(cacheKey, value);
  }

  private <T> void putFallback(CacheKey<T> cacheKey, T value, Throwable t) {
    log.warn("Redis 서킷 Open, put 스킵: key={}", cacheKey.key());
  }

  @Override
  @CircuitBreaker(name = "redis-cache", fallbackMethod = "evictFallback")
  public void evict(CacheKey<?> cacheKey) {
    delegate.evict(cacheKey);
  }

  private void evictFallback(CacheKey<?> cacheKey, Throwable t) {
    log.warn("Redis 서킷 Open, evict 스킵: key={}", cacheKey.key());
  }

  @Override
  @CircuitBreaker(name = "redis-cache", fallbackMethod = "getOrLoadFallback")
  public <T> T getOrLoad(CacheKey<T> cacheKey, Supplier<T> loader) {
    return delegate.getOrLoad(cacheKey, loader);
  }

  private <T> T getOrLoadFallback(CacheKey<T> cacheKey, Supplier<T> loader, Throwable t) {
    log.warn("Redis 서킷 Open, DB 직접 조회: key={}", cacheKey.key());
    return loader.get();
  }
}
