package com.loopers.domain.ranking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis ZSET 기반 랭킹 서비스
 * <p>
 * 일간 랭킹을 관리하며, 다음과 같은 기능을 제공합니다:
 * - ZSET 점수 증가 (ZINCRBY)
 * - Top-N 조회 (ZREVRANGE)
 * - 개별 상품 순위 조회 (ZREVRANK)
 * - 개별 상품 점수 조회 (ZSCORE)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingService {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final long TTL_SECONDS = 2 * 24 * 60 * 60; // 2일

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 일간 랭킹 키 생성
     *
     * @param date yyyyMMdd 형식의 날짜 문자열
     * @return ranking:all:{yyyyMMdd}
     */
    public String getRankingKey(String date) {
        return RANKING_KEY_PREFIX + date;
    }

    /**
     * 오늘 날짜의 랭킹 키 반환
     */
    public String getTodayRankingKey() {
        return getRankingKey(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * ZSET에 점수 증가 (ZINCRBY)
     *
     * @param key       랭킹 키
     * @param productId 상품 ID (member)
     * @param score     증가할 점수
     * @return 증가 후 총 점수
     */
    public Double incrementScore(String key, Long productId, double score) {
        // TTL 설정 (키가 없을 때만)
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        }

        return redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score);
    }

    /**
     * Top-N 상품 ID 조회 (ZREVRANGE)
     *
     * @param key   랭킹 키
     * @param start 시작 인덱스 (0부터)
     * @param end   종료 인덱스 (N-1)
     * @return 상품 ID 리스트 (점수 내림차순)
     */
    public List<Long> getTopNProductIds(String key, long start, long end) {
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, start, end);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * Top-N 상품 ID와 점수 조회 (ZREVRANGE WITHSCORES)
     */
    public List<RankingEntry> getTopNWithScores(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        return tuples.stream()
                .map(tuple -> new RankingEntry(
                        Long.parseLong(tuple.getValue()),
                        tuple.getScore() != null ? tuple.getScore() : 0.0
                ))
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 순위 조회 (ZREVRANK)
     *
     * @param key       랭킹 키
     * @param productId 상품 ID
     * @return 순위 (0부터 시작, null이면 랭킹에 없음)
     */
    public Long getRank(String key, Long productId) {
        return redisTemplate.opsForZSet().reverseRank(key, productId.toString());
    }

    /**
     * 특정 상품의 점수 조회 (ZSCORE)
     */
    public Double getScore(String key, Long productId) {
        return redisTemplate.opsForZSet().score(key, productId.toString());
    }

    /**
     * 랭킹에 포함된 상품 수 조회 (ZCARD)
     */
    public Long getRankingSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0L;
    }

    /**
     * TTL 설정
     */
    public void setTtl(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 랭킹 엔트리 (상품 ID + 점수)
     */
    @Getter
    @AllArgsConstructor
    public static class RankingEntry {
        private Long productId;
        private Double score;
    }
}

