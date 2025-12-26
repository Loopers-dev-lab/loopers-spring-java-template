package com.loopers.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 회원별 좋아요 상품 목록 캐시
 * Redis SET 사용: likes:member:{memberId}
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MemberLikesCache {

    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> cacheRedisTemplate;

    /**
     * 좋아요 추가
     */
    public void add(Long memberId, Long productId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            cacheRedisTemplate.opsForSet().add(key, productId);
            cacheRedisTemplate.expire(key, TTL);
            log.debug("[MemberLikesCache] add memberId={}, productId={}", memberId, productId);
        } catch (Exception e) {
            log.warn("[MemberLikesCache] add failed, error={}", e.getMessage());
        }
    }

    /**
     * 좋아요 취소
     */
    public void remove(Long memberId, Long productId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            cacheRedisTemplate.opsForSet().remove(key, productId);
            log.debug("[MemberLikesCache] remove memberId={}, productId={}", memberId, productId);
        } catch (Exception e) {
            log.warn("[MemberLikesCache] remove failed, error={}", e.getMessage());
        }
    }

    /**
     * 좋아요 여부 확인 (단건)
     */
    public boolean isMember(Long memberId, Long productId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            Boolean result = cacheRedisTemplate.opsForSet().isMember(key, productId);
            return result != null && result;
        } catch (Exception e) {
            log.warn("[MemberLikesCache] isMember failed, error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 여러 상품 중 좋아요한 상품 ID 조회 (배치)
     */
    public Set<Long> findLikedProductIds(Long memberId, List<Long> productIds) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);

            // 전체 좋아요 목록 가져오기
            Set<Object> allLiked = cacheRedisTemplate.opsForSet().members(key);

            if (allLiked == null || allLiked.isEmpty()) {
                return Set.of();
            }

            // productIds 중 좋아요한 것만 필터링 (변환 실패한 값은 제외)
            Set<Long> likedSet = allLiked.stream()
                    .map(this::toLongOrNull)
                    .filter(v -> v != null)
                    .collect(Collectors.toSet());

            return productIds.stream()
                    .filter(likedSet::contains)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.warn("[MemberLikesCache] findLikedProductIds failed, error={}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * 회원의 전체 좋아요 목록 조회
     */
    public Set<Long> findAll(Long memberId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            Set<Object> members = cacheRedisTemplate.opsForSet().members(key);

            if (members == null) {
                return Set.of();
            }

            return members.stream()
                    .map(this::toLongOrNull)
                    .filter(v -> v != null)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.warn("[MemberLikesCache] findAll failed, error={}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists(Long memberId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            Long size = cacheRedisTemplate.opsForSet().size(key);
            return size != null && size >= 0;
        } catch (Exception e) {
            log.warn("[MemberLikesCache] exists failed, error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 캐시 초기화 (DB 데이터로 캐시 워밍)
     */
    public void initialize(Long memberId, Set<Long> productIds) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);

            if (productIds.isEmpty()) {
                // 빈 SET 생성 (캐시 miss와 실제 좋아요 0개 구분)
                cacheRedisTemplate.opsForSet().add(key, -1L);  // 더미 값
                cacheRedisTemplate.opsForSet().remove(key, -1L);  // 즉시 제거
                cacheRedisTemplate.expire(key, TTL);
            } else {
                cacheRedisTemplate.opsForSet().add(key, productIds.toArray());
                cacheRedisTemplate.expire(key, TTL);
            }

            log.debug("[MemberLikesCache] initialize memberId={}, count={}", memberId, productIds.size());
        } catch (Exception e) {
            log.warn("[MemberLikesCache] initialize failed, error={}", e.getMessage());
        }
    }

    /**
     * 캐시 삭제
     */
    public void delete(Long memberId) {
        try {
            String key = CacheKeyGenerator.memberLikesKey(memberId);
            cacheRedisTemplate.delete(key);
            log.debug("[MemberLikesCache] delete memberId={}", memberId);
        } catch (Exception e) {
            log.warn("[MemberLikesCache] delete failed, error={}", e.getMessage());
        }
    }

    /**
     * Redis 값을 Long으로 변환 (변환 실패 시 null 반환)
     */
    private Long toLongOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("[MemberLikesCache] Failed to parse String to Long: {}", value);
                return null;
            }
        }
        log.warn("[MemberLikesCache] Unsupported value type: {}", value.getClass().getSimpleName());
        return null;
    }
}
