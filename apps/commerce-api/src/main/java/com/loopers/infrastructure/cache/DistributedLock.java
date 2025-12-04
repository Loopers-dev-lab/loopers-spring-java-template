package com.loopers.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class DistributedLock {

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_EXPIRE_TIME = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public DistributedLock(@Qualifier("redisTemplateMaster") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String RELEASE_LOCK_SCRIPT = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

    public LockHandle tryLock(String key) {
        return tryLock(key, DEFAULT_TIMEOUT, DEFAULT_EXPIRE_TIME);
    }

    public LockHandle tryLock(String key, Duration timeout, Duration expireTime) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long timeoutMillis = timeout.toMillis();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired: {}", lockKey);
                return new LockHandle(lockKey, lockValue);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        log.debug("Lock acquisition timeout: {}", lockKey);
        return null;
    }

    public void releaseLock(LockHandle lockHandle) {
        if (lockHandle == null) {
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RELEASE_LOCK_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, List.of(lockHandle.key()), lockHandle.value());
        if (result != null && result > 0) {
            log.debug("Lock released: {}", lockHandle.key());
        } else {
            log.warn("Lock release failed or already released: {}", lockHandle.key());
        }
    }

    public record LockHandle(String key, String value) {
    }
}

