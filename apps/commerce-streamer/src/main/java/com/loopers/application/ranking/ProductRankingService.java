package com.loopers.application.ranking;

import com.loopers.config.ranking.RankingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
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
     * 주문 점수 증가 (로그 적용)
     */
    public void incrementOrderScore(Long productId, double price, int quantity) {
        String key = getTodayKey();
        
        // log10(price * quantity + 1) * 0.7
        double score = Math.log10(price * quantity + 1) * rankingProperties.weights().order();
        
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
        redisTemplate.expire(key, Duration.ofDays(rankingProperties.ttlDays()));
        
        log.debug("Incremented order score for productId {}: score {}", productId, score);
    }

    /**
     * 좋아요 점수 증가
     */
    public void incrementLikeScore(Long productId) {
        String key = getTodayKey();
        double score = rankingProperties.weights().like();
        
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
        redisTemplate.expire(key, Duration.ofDays(rankingProperties.ttlDays()));
        
        log.debug("Incremented like score for productId {}: score {}", productId, score);
    }

    /**
     * 좋아요 점수 감소
     */
    public void decrementLikeScore(Long productId) {
        String key = getTodayKey();
        double score = -rankingProperties.weights().like();
        
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
        redisTemplate.expire(key, Duration.ofDays(rankingProperties.ttlDays()));
        
        log.debug("Decremented like score for productId {}: score {}", productId, score);
    }

    /**
     * 조회수 점수 증가
     */
    public void incrementViewScore(Long productId) {
        String key = getTodayKey();
        double score = rankingProperties.weights().view();
        
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
        redisTemplate.expire(key, Duration.ofDays(rankingProperties.ttlDays()));
        
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

    /**
     * 전날 점수의 일부를 오늘 랭킹에 이월
     */
    public void carryOverPreviousDayScore() {
        if (!rankingProperties.carryOver().enabled()) {
            log.info("Score Carry-Over is disabled, skipping...");
            return;
        }
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        
        String yesterdayKey = getKeyForDate(yesterday);
        String todayKey = getTodayKey();
        
        // 전날 키가 존재하는지 확인
        Long yesterdaySize = redisTemplate.opsForZSet().zCard(yesterdayKey);
        if (yesterdaySize == null || yesterdaySize == 0) {
            log.info("No ranking data found for yesterday ({}), skipping carry-over", yesterdayKey);
            return;
        }
        
        // ZUNIONSTORE로 전날 점수의 일부를 오늘 키로 복사
        // WEIGHTS 0.1 = 전날 점수의 10%만 이월
        // Spring Data Redis는 weights를 직접 지원하지 않으므로, 
        // 각 멤버의 점수를 읽어서 0.1을 곱한 후 저장
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .rangeWithScores(yesterdayKey, 0, -1);
        
        if (tuples != null && !tuples.isEmpty()) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String productId = tuple.getValue();
                Double score = tuple.getScore();
                
                if (productId != null && score != null) {
                    // 전날 점수의 10%를 오늘 키에 추가
                    double carryOverScore = score * rankingProperties.carryOver().weight();
                    redisTemplate.opsForZSet().incrementScore(todayKey, productId, carryOverScore);
                }
            }
        }
        
        // 오늘 키에 TTL 설정
        redisTemplate.expire(todayKey, Duration.ofDays(rankingProperties.ttlDays()));
        
        log.info("Score Carry-Over completed: {} -> {} (weight: {})", 
            yesterdayKey, todayKey, rankingProperties.carryOver().weight());
    }
}

