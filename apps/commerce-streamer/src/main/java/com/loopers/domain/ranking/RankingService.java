package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RankingWeight rankingWeight;

    private static final Duration RANKING_TTL = Duration.ofHours(48);

    /**
     * 조회 이벤트로 인한 랭킹 점수 증가
     */
    public void incrementViewScore(Long productId, LocalDate date) {
        double score = rankingWeight.calculateViewScore();
        incrementScore(productId, score, date);
    }

    /**
     * 좋아요 이벤트로 인한 랭킹 점수 변경
     */
    public void updateLikeScore(Long productId, boolean isLike, LocalDate date) {
        double score = rankingWeight.calculateLikeScore(isLike);
        incrementScore(productId, score, date);
    }

    /**
     * 주문 이벤트로 인한 랭킹 점수 증가 (수량 기반)
     */
    public void incrementOrderScore(Long productId, int quantity, LocalDate date) {
        double score = rankingWeight.calculateOrderScore(quantity);
        incrementScore(productId, score, date);
    }

    /**
     * 주문 이벤트로 인한 랭킹 점수 증가 (금액 기반)
     */
    public void incrementOrderScoreWithAmount(Long productId, long amount, LocalDate date) {
        double score = rankingWeight.calculateOrderScoreWithAmount(amount);
        incrementScore(productId, score, date);
    }

    /**
     * 랭킹 점수 증가 (내부 메서드)
     */
    private void incrementScore(Long productId, double score, LocalDate date) {
        String key = RankingKey.daily(date);
        String member = productId.toString();

        try {
            // 먼저 키 존재 여부 확인 (TTL 설정을 위해)
            Boolean keyExists = redisTemplate.hasKey(key);

            redisTemplate.opsForZSet().incrementScore(key, member, score);

            // 키가 새로 생성된 경우에만 TTL 설정
            if (Boolean.FALSE.equals(keyExists)) {
                redisTemplate.expire(key, RANKING_TTL);
                log.debug("랭킹 키 생성 및 TTL 설정: key={}, ttl={}", key, RANKING_TTL);
            }

            log.debug("랭킹 점수 업데이트: key={}, productId={}, score={}", key, productId, score);
        } catch (Exception e) {
            log.error("랭킹 점수 업데이트 실패: key={}, productId={}", key, productId, e);
        }
    }

    /**
     * 랭킹에 진입한 상품 수 조회
     */
    public Long getRankingSize(LocalDate date) {
        String key = RankingKey.daily(date);

        try {
            Long size = redisTemplate.opsForZSet().zCard(key);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("랭킹 사이즈 조회 실패: key={}", key, e);
            return 0L;
        }
    }

    /**
     * 콜드 스타트 대응: 전날 점수의 일부를 다음날 키에 복사
     */
    public void carryOverScores(LocalDate fromDate, LocalDate toDate, double weight) {
        String fromKey = RankingKey.daily(fromDate);
        String toKey = RankingKey.daily(toDate);

        try {
            // 이미 준비된 경우 스킵
            Long existingSize = getRankingSize(toDate);
            if (existingSize != null && existingSize > 0) {
                log.info("이미 준비된 랭킹이 존재합니다: key={}, size={}", toKey, existingSize);
                return;
            }

            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .rangeWithScores(fromKey, 0, -1);

            if (tuples == null || tuples.isEmpty()) {
                log.info("Carry-over 대상 없음: fromKey={}", fromKey);
                return;
            }

            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() != null && tuple.getScore() != null) {
                    double newScore = tuple.getScore() * weight;
                    redisTemplate.opsForZSet().add(toKey, tuple.getValue(), newScore);
                }
            }

            redisTemplate.expire(toKey, RANKING_TTL);

            log.info("랭킹 Carry-over 완료: {} → {} (weight={}), count={}",
                    fromKey, toKey, weight, tuples.size());
        } catch (Exception e) {
            log.error("랭킹 Carry-over 실패: fromKey={}, toKey={}", fromKey, toKey, e);
        }
    }
}
