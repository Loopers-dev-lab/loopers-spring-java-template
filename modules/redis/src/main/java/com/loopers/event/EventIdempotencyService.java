package com.loopers.event;

import com.loopers.config.redis.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 이벤트 멱등성 처리를 위한 서비스
 * Redis SETNX + TTL을 사용하여 중복 이벤트를 방지합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIdempotencyService {

    private static final String KEY_PREFIX = "event:dedup:";
    private static final Duration TTL = Duration.ofDays(1); // 24시간

    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 이벤트 ID에 대한 중복 체크를 수행합니다.
     * 
     * @param eventId 이벤트 고유 ID
     * @return true: 중복이 아님 (처리 가능), false: 중복 (이미 처리됨)
     */
    public boolean tryAcquire(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("EventId is null or blank, allowing processing");
            return true;
        }

        try {
            String key = KEY_PREFIX + eventId;
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
            
            if (result == null) {
                // Redis 장애 시 fail-open: 로그 경고 후 처리 허용
                log.warn("Redis SETNX returned null for eventId: {}, allowing processing (fail-open)", eventId);
                return true;
            }
            
            return result;
        } catch (Exception e) {
            // Redis 장애 시 fail-open: 로그 경고 후 처리 허용
            log.error("Redis error during idempotency check for eventId: {}, allowing processing (fail-open)", eventId, e);
            return true;
        }
    }
}

