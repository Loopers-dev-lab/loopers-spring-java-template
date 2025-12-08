package com.loopers.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCacheTemplate implements CacheTemplate {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public <T> Optional<T> get(CacheKey<T> cacheKey) {
    String json = redisTemplate.opsForValue().get(cacheKey.key());
    if (json == null) {
      return Optional.empty();
    }
    T value = deserialize(json, cacheKey.type());
    return Optional.ofNullable(value);
  }

  @Override
  public <T> void put(CacheKey<T> cacheKey, T value) {
    String json = serialize(value);
    redisTemplate.opsForValue().set(cacheKey.key(), json, cacheKey.ttl());
  }

  @Override
  public void evict(CacheKey<?> cacheKey) {
    redisTemplate.delete(cacheKey.key());
  }

  @Override
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

  private String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new CacheSerializationException("캐시 직렬화 실패", e);
    }
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new CacheSerializationException("캐시 역직렬화 실패", e);
    }
  }
}
