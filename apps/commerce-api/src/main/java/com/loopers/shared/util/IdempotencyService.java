package com.loopers.shared.util;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.loopers.config.redis.RedisConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IdempotencyService {
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long TTL_SECONDS = 10L;
    
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 멱등성 키 체크 및 설정 (원자적 연산)
     * @param idempotencyKey 멱등 키
     * @return 이미 존재하면 true (중복 요청), 존재하지 않으면 false (새로운 요청)
     */
    public boolean checkAndSet(IdempotencyType type, String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + type.getValueString() + ":" + idempotencyKey;
        
        // SET key value NX EX ttl: 키가 없으면 설정하고 TTL 적용 (원자적 연산)
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
            redisKey,
            String.valueOf(System.currentTimeMillis()),  // 처리 시작 시간 저장
            TTL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // isNew가 null이면 Redis 오류, false면 중복 요청
        return !Boolean.TRUE.equals(isNew);
    }

    /**
     * 멱등성 키 삭제
     */
    public void delete(IdempotencyType type, String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + type.getValueString() + ":" + idempotencyKey;
        redisTemplate.delete(redisKey);
    }
}
