package com.loopers.utils;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 테스트 헬퍼
 * - Redis 데이터 초기화
 * - Redis 연결 상태 확인
 */
@Component
public class RedisTestHelper {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTestHelper(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Redis의 모든 데이터 삭제 (현재 DB만)
     */
    public void clearAll() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.flushDb();
            return null;
        });
    }

    /**
     * Redis의 모든 DB 데이터 삭제 (주의: 모든 DB)
     */
    public void clearAllDatabases() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.flushAll();
            return null;
        });
    }

    /**
     * Redis 연결 상태 확인
     *
     * @return 연결 가능 여부
     */
    public boolean isConnected() {
        try {
            return redisTemplate.execute((RedisConnection connection) -> {
                connection.ping();
                return true;
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 특정 패턴의 키 삭제
     *
     * @param pattern 삭제할 키 패턴 (예: "product:like:*")
     */
    public void deleteByPattern(String pattern) {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.keys(pattern.getBytes()).forEach(connection::del);
            return null;
        });
    }

    /**
     * Redis에 저장된 전체 키 개수 반환
     */
    public long getKeyCount() {
        return redisTemplate.execute((RedisConnection connection) ->
            connection.dbSize()
        );
    }
}
