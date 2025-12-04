package com.loopers.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductStock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Repository
public class RedisProductCacheRepository {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String CACHE_KEY_PREFIX = "product:detail:";

  public List<Long> getIdList(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) return null;

    try {
      return objectMapper.readValue(json, new TypeReference<List<Long>>() {
      });
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void saveIdList(String key, List<Long> ids, Duration ttl) {
    try {
      String json = objectMapper.writeValueAsString(ids);
      redisTemplate.opsForValue().set(key, json, ttl);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  public void evictByPrefix(String prefix) {
    Set<String> keys = redisTemplate.keys(prefix + "*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  public ProductStock get(Long productId) {
    String key = CACHE_KEY_PREFIX + productId;
    String json = redisTemplate.opsForValue().get(key);

    if (json == null) return null;

    try {
      return objectMapper.readValue(json, ProductStock.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void save(ProductStock productStock, Duration ttl) {
    String key = CACHE_KEY_PREFIX + productStock.product().getId();
    try {
      String json = objectMapper.writeValueAsString(productStock);
      redisTemplate.opsForValue().set(key, json, ttl);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  public void evict(Long productId) {
    String key = CACHE_KEY_PREFIX + productId;
    redisTemplate.delete(key);
  }
}
