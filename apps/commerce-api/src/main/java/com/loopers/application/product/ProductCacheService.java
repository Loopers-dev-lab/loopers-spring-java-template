package com.loopers.application.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis를 활용한 상품 관련 실시간 데이터 관리
 *
 * Spring Cache (@Cacheable)와 다른 점:
 * - 세밀한 캐시 제어 가능
 * - 조회수, 좋아요 수 등 실시간 카운팅 가능
 * - 캐시 히트/미스 로깅 가능
 *
 * 사용 예시:
 * - 상품 조회수 실시간 카운팅
 * - 좋아요 수 실시간 동기화 (선택적)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PRODUCT_VIEW_COUNT_KEY = "product:view:";
    private static final String PRODUCT_LIKE_COUNT_KEY = "product:like:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * 상품 조회수 증가
     *
     * 사용 시점: 상품 상세 조회 API 호출 시
     * Redis Incr 사용: 원자성 보장, 동시성 안전
     */
    public Long incrementViewCount(Long productId) {
        String key = PRODUCT_VIEW_COUNT_KEY + productId;
        Long count = redisTemplate.opsForValue().increment(key);

        // TTL 설정 (첫 생성 시)
        if (count != null && count == 1) {
            redisTemplate.expire(key, CACHE_TTL);
        }

        log.debug("Product {} view count incremented to {}", productId, count);
        return count;
    }

    /**
     * 상품 조회수 조회
     *
     * Redis에서 먼저 조회, 없으면 0 반환
     */
    public Long getViewCount(Long productId) {
        String key = PRODUCT_VIEW_COUNT_KEY + productId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 좋아요 수 캐시 (선택적 사용)
     *
     * 장점: DB 부하 감소, 실시간 반영
     * 단점: DB와 동기화 필요
     *
     * 현재는 Spring Cache로 충분하므로 선택적으로 사용
     */
    public void cacheLikeCount(Long productId, Integer likeCount) {
        String key = PRODUCT_LIKE_COUNT_KEY + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(likeCount), Duration.ofMinutes(5));
        log.debug("Product {} like count cached: {}", productId, likeCount);
    }

    /**
     * 좋아요 수 조회 (캐시)
     */
    public Optional<Integer> getCachedLikeCount(Long productId) {
        String key = PRODUCT_LIKE_COUNT_KEY + productId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of(Integer.parseInt(value)) : Optional.empty();
    }

    /**
     * 좋아요 수 증가 (실시간)
     *
     * 사용 시 주의:
     * - DB와 Redis 모두 업데이트 필요
     * - 트랜잭션 경계 고려
     */
    public Long incrementLikeCount(Long productId) {
        String key = PRODUCT_LIKE_COUNT_KEY + productId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(5));
        }

        log.debug("Product {} like count incremented in Redis to {}", productId, count);
        return count;
    }

    /**
     * 좋아요 수 감소 (실시간)
     */
    public Long decrementLikeCount(Long productId) {
        String key = PRODUCT_LIKE_COUNT_KEY + productId;
        Long count = redisTemplate.opsForValue().decrement(key);

        log.debug("Product {} like count decremented in Redis to {}", productId, count);
        return count != null ? Math.max(0, count) : 0;
    }

    /**
     * 캐시 삭제 (무효화)
     */
    public void evictLikeCount(Long productId) {
        String key = PRODUCT_LIKE_COUNT_KEY + productId;
        redisTemplate.delete(key);
        log.debug("Product {} like count cache evicted", productId);
    }

    /**
     * 캐시 통계 (모니터링용)
     *
     * 장점: 캐시 효율 측정 가능
     */
    public CacheStats getCacheStats(Long productId) {
        String viewKey = PRODUCT_VIEW_COUNT_KEY + productId;
        String likeKey = PRODUCT_LIKE_COUNT_KEY + productId;

        boolean viewCached = Boolean.TRUE.equals(redisTemplate.hasKey(viewKey));
        boolean likeCached = Boolean.TRUE.equals(redisTemplate.hasKey(likeKey));

        return new CacheStats(viewCached, likeCached);
    }

    public record CacheStats(boolean viewCountCached, boolean likeCountCached) {}
}
