package com.loopers.application.ranking;

import com.loopers.config.ranking.RankingProperties;
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
    
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final String RANKING_HOURLY_KEY = "ranking:hourly";
    private static final String RANKING_DAILY_KEY = "ranking:daily";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 오늘 날짜 기반 랭킹 키 생성
     */
    private String getTodayKey() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + date;
    }

    /**
     * 특정 날짜의 랭킹 키 생성
     */
    private String getKeyForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + dateStr;
    }

    /**
     * 주문 점수 계산
     */
    public double calculateOrderScore(double price, int quantity) {
        // log10(price * quantity + 1) * 0.7
        return Math.log10(price * quantity + 1) * rankingProperties.weights().order();
    }

    /**
     * 주문 점수 증가 (로그 적용)
     * 실시간으로 daily 키와 hourly 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementOrderScore(Long productId, double price, int quantity) {
        String todayKey = getTodayKey();
        double score = calculateOrderScore(price, quantity);
        String productIdStr = productId.toString();
        
        // 기존 daily 키 (날짜별)
        redisTemplate.opsForZSet().incrementScore(todayKey, productIdStr, score);
        redisTemplate.expire(todayKey, Duration.ofDays(rankingProperties.ttlDays()));
        
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
     */
    public double getLikeScore() {
        return rankingProperties.weights().like();
    }

    /**
     * 조회수 점수 반환
     */
    public double getViewScore() {
        return rankingProperties.weights().view();
    }

    /**
     * 좋아요 점수 증가
     * 실시간으로 daily 키와 hourly 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementLikeScore(Long productId) {
        String todayKey = getTodayKey();
        double score = getLikeScore();
        String productIdStr = productId.toString();
        
        // 기존 daily 키 (날짜별)
        redisTemplate.opsForZSet().incrementScore(todayKey, productIdStr, score);
        redisTemplate.expire(todayKey, Duration.ofDays(rankingProperties.ttlDays()));
        
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
     * 실시간으로 daily 키와 hourly 키에 모두 점수 차감 (슬라이딩 윈도우용)
     */
    public void decrementLikeScore(Long productId) {
        String todayKey = getTodayKey();
        double score = -getLikeScore();
        String productIdStr = productId.toString();
        
        // 기존 daily 키 (날짜별)
        redisTemplate.opsForZSet().incrementScore(todayKey, productIdStr, score);
        redisTemplate.expire(todayKey, Duration.ofDays(rankingProperties.ttlDays()));
        
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
     * 실시간으로 daily 키와 hourly 키에 모두 점수 추가 (슬라이딩 윈도우용)
     */
    public void incrementViewScore(Long productId) {
        String todayKey = getTodayKey();
        double score = getViewScore();
        String productIdStr = productId.toString();
        
        // 기존 daily 키 (날짜별)
        redisTemplate.opsForZSet().incrementScore(todayKey, productIdStr, score);
        redisTemplate.expire(todayKey, Duration.ofDays(rankingProperties.ttlDays()));
        
        // 슬라이딩 윈도우용 hourly 키 (최근 1시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_HOURLY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_HOURLY_KEY, Duration.ofHours(2));
        
        // 슬라이딩 윈도우용 daily 키 (최근 24시간)
        redisTemplate.opsForZSet().incrementScore(RANKING_DAILY_KEY, productIdStr, score);
        redisTemplate.expire(RANKING_DAILY_KEY, Duration.ofHours(25));
        
        log.debug("Incremented view score for productId {}: score {}", productId, score);
    }

    /**
     * Top-N 상품 ID 조회 (순위만)
     */
    public List<Long> getTopProductIds(LocalDate date, int limit) {
        String key = getKeyForDate(date);
        Set<String> productIds = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return productIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 순위 조회 (1-based)
     */
    public Long getProductRank(Long productId, LocalDate date) {
        String key = getKeyForDate(date);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, productId.toString());
        
        if (rank == null) {
            return null;
        }
        
        // 0-based를 1-based로 변환
        return rank + 1;
    }

}

