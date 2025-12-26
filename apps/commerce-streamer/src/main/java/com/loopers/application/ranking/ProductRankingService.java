package com.loopers.application.ranking;

import com.loopers.config.ranking.RankingProperties;
import com.loopers.domain.ranking.RankingEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final StringRedisTemplate redisTemplate;
    private final RankingProperties rankingProperties;
    private final RankingWeightService rankingWeightService;
    
    private static final String RANKING_HOURLY_KEY = "ranking:hourly";
    private static final String RANKING_DAILY_KEY = "ranking:daily";

    /**
     * 주문 점수 계산
     * 동적 weight 적용
     */
    public double calculateOrderScore(double price, int quantity) {
        // 동적으로 weight 조회
        double orderWeight = rankingWeightService.getWeight(RankingEventType.ORDER);
        // log10(price * quantity + 1) * weight
        return Math.log10(price * quantity + 1) * orderWeight;
    }

    /**
     * 주문 점수 증가 (로그 적용)
     * 실시간으로 hourly 키와 daily 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementOrderScore(Long productId, double price, int quantity) {
        double score = calculateOrderScore(price, quantity);
        String productIdStr = productId.toString();
        
        // 슬라이딩 윈도우용 hourly 키 (최근 1시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_HOURLY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_HOURLY_KEY, Duration.ofHours(2));
        
        // 슬라이딩 윈도우용 daily 키 (최근 24시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_DAILY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_DAILY_KEY, Duration.ofHours(25));
        
        log.debug("Incremented order score for productId {}: score {}", productId, score);
    }

    /**
     * 좋아요 점수 반환
     * 동적 weight 적용
     */
    public double getLikeScore() {
        return rankingWeightService.getWeight(RankingEventType.LIKE);
    }

    /**
     * 조회수 점수 반환
     * 동적 weight 적용
     */
    public double getViewScore() {
        return rankingWeightService.getWeight(RankingEventType.VIEW);
    }

    /**
     * 좋아요 점수 증가
     * 실시간으로 hourly 키와 daily 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementLikeScore(Long productId) {
        double score = getLikeScore();
        String productIdStr = productId.toString();
        
        // 슬라이딩 윈도우용 hourly 키 (최근 1시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_HOURLY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_HOURLY_KEY, Duration.ofHours(2));
        
        // 슬라이딩 윈도우용 daily 키 (최근 24시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_DAILY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_DAILY_KEY, Duration.ofHours(25));
        
        log.debug("Incremented like score for productId {}: score {}", productId, score);
    }

    /**
     * 좋아요 점수 감소
     * 실시간으로 hourly 키와 daily 키에 모두 점수 차감 (슬라이딩 윈도우용)
     */
    public void decrementLikeScore(Long productId) {
        double score = -getLikeScore();
        String productIdStr = productId.toString();
        
        // 슬라이딩 윈도우용 hourly 키 (최근 1시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_HOURLY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_HOURLY_KEY, Duration.ofHours(2));
        
        // 슬라이딩 윈도우용 daily 키 (최근 24시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_DAILY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_DAILY_KEY, Duration.ofHours(25));
        
        log.debug("Decremented like score for productId {}: score {}", productId, score);
    }

    /**
     * 조회수 점수 증가
     * 실시간으로 hourly 키와 daily 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementViewScore(Long productId) {
        double score = getViewScore();
        String productIdStr = productId.toString();
        
        // 슬라이딩 윈도우용 hourly 키 (최근 1시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_HOURLY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_HOURLY_KEY, Duration.ofHours(2));
        
        // 슬라이딩 윈도우용 daily 키 (최근 24시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_DAILY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_DAILY_KEY, Duration.ofHours(25));
        
        log.debug("Incremented view score for productId {}: score {}", productId, score);
    }

}

