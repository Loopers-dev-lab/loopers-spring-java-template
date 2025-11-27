package com.loopers.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheTemplate {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  /** 캐시에서 조회. 실패 시 Optional.empty() 반환 */
  public <T> Optional<T> get(CacheKey<T> cacheKey) {
    try {
      String json = redisTemplate.opsForValue().get(cacheKey.key());
      if (json == null) {
        return Optional.empty();
      }
      T value = objectMapper.readValue(json, cacheKey.type());
      return Optional.ofNullable(value);
    } catch (Exception e) {
      log.warn("캐시 조회 실패: key={}", cacheKey.key(), e);
      return Optional.empty();
    }
  }

  /** 캐시에 저장. TTL은 CacheKey에 정의됨 */
  public <T> void put(CacheKey<T> cacheKey, T value) {
    try {
      String json = objectMapper.writeValueAsString(value);
      redisTemplate.opsForValue().set(cacheKey.key(), json, cacheKey.ttl());
    } catch (Exception e) {
      log.warn("캐시 저장 실패: key={}", cacheKey.key(), e);
    }
  }

  /** 캐시 삭제 (무효화) */
  public void evict(CacheKey<?> cacheKey) {
    try {
      redisTemplate.delete(cacheKey.key());
    } catch (Exception e) {
      log.warn("캐시 삭제 실패: key={}", cacheKey.key(), e);
    }
  }

  /** 캐시 조회 → 없으면 loader 실행 → 결과 캐시 저장 (Cache-Aside 패턴) */
  public <T> T getOrLoad(CacheKey<T> cacheKey, Supplier<T> loader) {
    Optional<T> cached = get(cacheKey);
    if (cached.isPresent()) {
      return cached.get();
    }

    T value = loader.get();
    if (value != null) {
      put(cacheKey, value);
    }
    return value;
  }
}
