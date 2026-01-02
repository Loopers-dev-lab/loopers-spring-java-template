package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${ranking.ttl-days:2}")
    private int rankingTtlDays;

    @Value("${ranking.weight.like:0.2}")
    private double likeWeight;

    @Value("${ranking.weight.view:0.1}")
    private double viewWeight;

    @Value("${ranking.weight.order:0.6}")
    private double orderWeight;

    private static final String LIKE_RANKING_KEY_PREFIX = "ranking:like";
    private static final String VIEW_RANKING_KEY_PREFIX = "ranking:view";
    private static final String ORDER_RANKING_KEY_PREFIX = "ranking:order";
    private static final String ALL_RANKING_KEY_PREFIX = "ranking:all";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 좋아요 랭킹 증분 업데이트 (가중치 적용)
     * - Score = delta × 0.2
     */
    public void incrementProductLikeRanking(Map<Long, Integer> likeDeltas) {
        incrementRanking(LIKE_RANKING_KEY_PREFIX, likeDeltas, likeWeight);
    }

    /**
     * 조회수 랭킹 증분 업데이트 (가중치 적용)
     * - Score = delta × 0.1
     */
    public void incrementProductViewRanking(Map<Long, Integer> viewDeltas) {
        incrementRanking(VIEW_RANKING_KEY_PREFIX, viewDeltas, viewWeight);
    }

    /**
     * 주문 랭킹 증분 업데이트 (가중치 적용)
     * - Score = delta × 0.6
     */
    public void incrementProductOrderRanking(Map<Long, Integer> orderDeltas) {
        incrementRanking(ORDER_RANKING_KEY_PREFIX, orderDeltas, orderWeight);
    }

    /**
     * 종합 랭킹 증분 업데이트 (가중치 합산)
     * - Score = (likeDelta × 0.2) + (viewDelta × 0.1) + (orderDelta × 0.6)
     */
    public void incrementProductAllRanking(Map<Long, Double> compositeScores) {
        incrementRankingWithCompositeScores(ALL_RANKING_KEY_PREFIX, compositeScores);
    }

    /**
     * 랭킹 증분 업데이트 공통 로직 (가중치 적용)
     * @param prefix 랭킹 키 prefix (like/view/order)
     * @param deltas 상품별 증감량
     * @param weight 가중치 (0.0 ~ 1.0)
     */
    private void incrementRanking(String prefix, Map<Long, Integer> deltas, double weight) {
        if (deltas == null || deltas.isEmpty()) {
            log.warn("증감량 없음, {} 랭킹 갱신 스킵", prefix);
            return;
        }

        String todayKey = prefix + ":" + LocalDate.now().format(DATE_FORMATTER);

        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    ZSetOperations<String, String> zSetOps = operations.opsForZSet();

                    for (Map.Entry<Long, Integer> entry : deltas.entrySet()) {
                        if (entry.getValue() == 0) continue;

                        // 가중치 적용: score = delta × weight
                        double score = entry.getValue() * weight;
                        zSetOps.incrementScore(todayKey, entry.getKey().toString(), score);
                    }

                    operations.expire(todayKey, rankingTtlDays, TimeUnit.DAYS);
                    return null;
                }
            });

            log.info("{} 랭킹 증분 업데이트 완료 (가중치: {}) - 키: {}, 항목 수: {}",
                    prefix, weight, todayKey, deltas.size());

        } catch (Exception e) {
            log.error("{} 랭킹 증분 업데이트 실패 - 키: {}", prefix, todayKey, e);
            throw new RuntimeException("랭킹 증분 업데이트 실패: " + todayKey, e);
        }
    }

    /**
     * 종합 점수로 랭킹 증분 업데이트 (가중치 이미 적용된 점수)
     * @param prefix 랭킹 키 prefix
     * @param compositeScores 상품별 종합 점수 (Double)
     */
    private void incrementRankingWithCompositeScores(String prefix, Map<Long, Double> compositeScores) {
        if (compositeScores == null || compositeScores.isEmpty()) {
            log.warn("종합 점수 없음, {} 랭킹 갱신 스킵", prefix);
            return;
        }

        String todayKey = prefix + ":" + LocalDate.now().format(DATE_FORMATTER);

        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    ZSetOperations<String, String> zSetOps = operations.opsForZSet();

                    for (Map.Entry<Long, Double> entry : compositeScores.entrySet()) {
                        if (entry.getValue() == 0.0) continue;

                        zSetOps.incrementScore(todayKey, entry.getKey().toString(), entry.getValue());
                    }

                    operations.expire(todayKey, rankingTtlDays, TimeUnit.DAYS);
                    return null;
                }
            });

            log.info("{} 랭킹 증분 업데이트 완료 - 키: {}, 항목 수: {}",
                    prefix, todayKey, compositeScores.size());

        } catch (Exception e) {
            log.error("{} 랭킹 증분 업데이트 실패 - 키: {}", prefix, todayKey, e);
            throw new RuntimeException("랭킹 증분 업데이트 실패: " + todayKey, e);
        }
    }
}
