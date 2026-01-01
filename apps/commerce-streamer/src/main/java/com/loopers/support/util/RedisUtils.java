package com.loopers.support.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisUtils {

  /**
   * TTL 설정 (키가 처음 생성된 경우에만)
   *
   * @param redisTemplate Redis template
   * @param key Redis 키
   * @param ttl TTL 값
   * @param timeUnit 시간 단위
   */
  public static void setTtlIfNeeded(RedisTemplate<String, String> redisTemplate, String key, long ttl, TimeUnit timeUnit) {
    try {
      Long currentTtl = redisTemplate.getExpire(key);
      if (currentTtl == -1) { // TTL이 설정되지 않은 경우
        redisTemplate.expire(key, ttl, timeUnit);
        log.debug("Set TTL for key: {}, ttl={} {}", key, ttl, timeUnit.toString().toLowerCase());
      }
    } catch (Exception e) {
      log.warn("Failed to set TTL for key: {}", key, e);
    }
  }

  /**
   * TTL을 일(day) 단위로 설정 (키가 처음 생성된 경우에만)
   *
   * @param redisTemplate Redis template
   * @param key Redis 키
   * @param days TTL 일수
   */
  public static void setTtlInDaysIfNeeded(RedisTemplate<String, String> redisTemplate, String key, long days) {
    setTtlIfNeeded(redisTemplate, key, days, TimeUnit.DAYS);
  }
}