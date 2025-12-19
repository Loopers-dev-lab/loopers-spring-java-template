package com.loopers.infrastructure.lock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 분산락 구현
 * <p>
 * Lua 스크립트를 사용하여 원자적 락 획득/해제를 보장합니다.
 * 
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else return 0 end";
    
    private static final DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>();
    
    static {
        UNLOCK_LUA_SCRIPT.setScriptText(UNLOCK_SCRIPT);
        UNLOCK_LUA_SCRIPT.setResultType(Long.class);
    }
    
    /**
     * 락 획득 시도
     * 
     * @param lockKey 락 키
     * @param lockValue 락 값 (보통 스레드 ID나 UUID)
     * @param expireTime 락 만료 시간
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        String key = LOCK_PREFIX + lockKey;
        
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, expireTime);
            
            boolean acquired = Boolean.TRUE.equals(result);
            
            if (acquired) {
                log.debug("분산락 획득 성공 - key: {}, value: {}", key, lockValue);
            } else {
                log.debug("분산락 획득 실패 - key: {}, value: {}", key, lockValue);
            }
            
            return acquired;
            
        } catch (Exception e) {
            log.error("분산락 획득 중 오류 발생 - key: {}, value: {}", key, lockValue, e);
            return false;
        }
    }
    
    /**
     * 락 해제
     * 
     * @param lockKey 락 키
     * @param lockValue 락 값 (획득 시 사용한 값과 동일해야 함)
     * @return 락 해제 성공 여부
     */
    public boolean unlock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;
        
        try {
            List<String> keys = new java.util.ArrayList<>();
            keys.add(key);
            Long result = redisTemplate.execute(
                    UNLOCK_LUA_SCRIPT,
                    keys,
                    lockValue
            );
            
            boolean released = Long.valueOf(1).equals(result);
            
            if (released) {
                log.debug("분산락 해제 성공 - key: {}, value: {}", key, lockValue);
            } else {
                log.debug("분산락 해제 실패 - key: {}, value: {} (이미 만료되었거나 다른 스레드가 소유)", key, lockValue);
            }
            
            return released;
            
        } catch (Exception e) {
            log.error("분산락 해제 중 오류 발생 - key: {}, value: {}", key, lockValue, e);
            return false;
        }
    }
    
    /**
     * 락을 획득하고 작업을 실행한 후 자동으로 해제
     * 
     * @param lockKey 락 키
     * @param lockValue 락 값
     * @param expireTime 락 만료 시간
     * @param maxWaitTime 최대 대기 시간
     * @param task 실행할 작업
     * @return 작업 실행 성공 여부
     */
    public boolean executeWithLock(String lockKey, String lockValue, Duration expireTime, 
                                 Duration maxWaitTime, Runnable task) {
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitTime.toMillis();
        
        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            if (tryLock(lockKey, lockValue, expireTime)) {
                try {
                    task.run();
                    return true;
                } finally {
                    unlock(lockKey, lockValue);
                }
            }
            
            // 더 짧은 시간 대기 후 재시도 (고성능 처리)
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("분산락 대기 중 인터럽트 발생 - key: {}", lockKey);
                return false;
            }
        }
        
        log.warn("분산락 획득 타임아웃 - key: {}, maxWaitTime: {}ms", lockKey, maxWaitMillis);
        return false;
    }
}
