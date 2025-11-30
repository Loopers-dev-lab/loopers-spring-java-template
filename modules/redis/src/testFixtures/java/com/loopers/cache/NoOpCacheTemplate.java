package com.loopers.cache;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트용 캐시 템플릿 - 캐시를 사용하지 않고 항상 loader 실행
 */
@Component
@Primary
@Profile("test")
public class NoOpCacheTemplate implements CacheTemplate {

  @Override
  public <T> Optional<T> get(CacheKey<T> cacheKey) {
    return Optional.empty();
  }

  @Override
  public <T> void put(CacheKey<T> cacheKey, T value) {
    // no-op
  }

  @Override
  public void evict(CacheKey<?> cacheKey) {
    // no-op
  }

  @Override
  public <T> T getOrLoad(CacheKey<T> cacheKey, Supplier<T> loader) {
    return loader.get();
  }
}
