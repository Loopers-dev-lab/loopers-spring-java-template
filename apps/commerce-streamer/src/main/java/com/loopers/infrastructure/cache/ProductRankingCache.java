package com.loopers.infrastructure.cache;

import com.loopers.config.RankingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 상품 랭킹 ZSET 캐시 (적재용 - commerce-streamer)
 * Key: ranking:all:v1:{yyyyMMdd}
 * Member: productId
 * Score: 가중치 적용된 점수
 */
@Slf4j
@Component
public class ProductRankingCache {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final RedisTemplate<String, Object> cacheRedisTemplate;
    private final RankingConfig rankingConfig;

    /**
     * TTL이 설정된 키를 추적 (Race Condition 방지)
     * 날짜가 바뀌면 자동으로 새 키에 대해 TTL 설정
     */
    private final AtomicReference<String> ttlInitializedKey = new AtomicReference<>();

    public ProductRankingCache(RedisTemplate<String, Object> cacheRedisTemplate, RankingConfig rankingConfig) {
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.rankingConfig = rankingConfig;
    }

    public void addViewScore(Long productId) {
        double score = rankingConfig.getWeight().getView();
        incrementScore(productId, score);
        log.debug("[Ranking] View score added - productId: {}, score: {}", productId, score);
    }

    public void addLikeScore(Long productId) {
        double score = rankingConfig.getWeight().getLike();
        incrementScore(productId, score);
        log.debug("[Ranking] Like score added - productId: {}, score: {}", productId, score);
    }

    /** 점수가 0 이하가 되어도 ZSET에서 자동 제거되지 않음 (일관성 유지) */
    public void subtractLikeScore(Long productId) {
        double score = rankingConfig.getWeight().getLike();
        decrementScore(productId, score);
        log.debug("[Ranking] Like score subtracted - productId: {}, score: -{}", productId, score);
    }

    /** log 정규화로 고가 상품의 과도한 점수 편중 방지: log1p(price * qty) */
    public void addOrderScore(Long productId, long price, int quantity) {
        double weight = rankingConfig.getWeight().getOrder();
        long orderAmount = price * quantity;
        // log 정규화: 가격 스케일 차이를 완화하여 공정한 랭킹 반영
        // 예: 100,000원 → log1p(100000) ≈ 11.5, 1,000원 → log1p(1000) ≈ 6.9
        double score = weight * Math.log1p(orderAmount);
        incrementScore(productId, score);
        log.debug("[Ranking] Order score added - productId: {}, price: {}, qty: {}, amount: {}, score: {}",
                productId, price, quantity, orderAmount, score);
    }

    /** Pipeline으로 여러 아이템을 한 번의 Redis 요청으로 처리 */
    public void addOrderScoresBatch(List<OrderItemScore> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        try {
            String key = getTodayKey();
            double weight = rankingConfig.getWeight().getOrder();

            // Pipeline으로 여러 ZINCRBY를 한 번에 실행
            cacheRedisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (OrderItemScore item : orderItems) {
                        long orderAmount = item.price() * item.quantity();
                        double score = weight * Math.log1p(orderAmount);
                        operations.opsForZSet().incrementScore(key, item.productId().toString(), score);
                    }
                    return null;
                }
            });

            // TTL 설정 (CAS로 원자적 처리)
            ensureTtl(key);

            log.debug("[Ranking] Batch order scores added - {} items", orderItems.size());
        } catch (Exception e) {
            log.warn("[Ranking] Failed to add batch order scores - error: {}", e.getMessage());
            // Fallback: 개별 처리
            for (OrderItemScore item : orderItems) {
                addOrderScore(item.productId(), item.price(), item.quantity());
            }
        }
    }

    public record OrderItemScore(Long productId, long price, int quantity) {}

    private void incrementScore(Long productId, double score) {
        try {
            String key = getTodayKey();
            cacheRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
            ensureTtl(key);
        } catch (Exception e) {
            log.warn("[Ranking] Failed to increment score - productId: {}, error: {}",
                    productId, e.getMessage());
        }
    }

    private void decrementScore(Long productId, double score) {
        try {
            String key = getTodayKey();
            // ZINCRBY는 음수 값으로 감소 가능
            cacheRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), -score);
            ensureTtl(key);
        } catch (Exception e) {
            log.warn("[Ranking] Failed to decrement score - productId: {}, error: {}",
                    productId, e.getMessage());
        }
    }

    /** getAndSet으로 키 변경 시에만 TTL 설정 (날짜가 바뀌는 시점) */
    private void ensureTtl(String key) {
        String oldKey = ttlInitializedKey.getAndSet(key);
        if (!key.equals(oldKey)) {
            cacheRedisTemplate.expire(key, rankingConfig.getTtlDays(), TimeUnit.DAYS);
            log.info("[Ranking] TTL set for key: {} ({} days)", key, rankingConfig.getTtlDays());
        }
    }

    /** Asia/Seoul 기준, commerce-api와 동일한 키 형식 */
    private String getTodayKey() {
        String date = LocalDate.now(ZONE_ID).format(DATE_FORMATTER);
        return CacheKeyGenerator.dailyRankingKey(date);
    }

    public static String getKeyForDate(String date) {
        return CacheKeyGenerator.dailyRankingKey(date);
    }
}
