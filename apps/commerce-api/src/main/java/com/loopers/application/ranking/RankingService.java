package com.loopers.application.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * 랭킹 조회 서비스 (Redis ZSET 조회)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DAILY_PREFIX = "ranking:all:";
    private static final String HOURLY_PREFIX = "ranking:realtime:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 일간 랭킹 Top-N 조회
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 랭킹 아이템 목록
     */
    public List<RankingItem> getDailyRanking(String date, int page, int size) {
        String key = DAILY_PREFIX + date;
        return getRanking(key, page, size);
    }

    /**
     * 오늘 일간 랭킹 Top-N 조회
     */
    public List<RankingItem> getTodayRanking(int page, int size) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return getDailyRanking(today, page, size);
    }

    /**
     * 특정 상품의 일간 랭킹 순위 조회
     *
     * @param date 조회 날짜
     * @param productId 상품 ID
     * @return 순위 (1부터 시작, 없으면 null)
     */
    public Long getProductRank(String date, Long productId) {
        String key = DAILY_PREFIX + date;
        String member = productId.toString();

        Long rank = redisTemplate.opsForZSet().reverseRank(key, member);
        return rank != null ? rank + 1 : null;  // 0-based → 1-based
    }

    /**
     * 오늘 특정 상품의 랭킹 순위 조회
     */
    public Long getProductRankToday(Long productId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return getProductRank(today, productId);
    }

    /**
     * 특정 상품의 점수 조회
     */
    public Double getProductScore(String date, Long productId) {
        String key = DAILY_PREFIX + date;
        String member = productId.toString();

        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * ZSET에서 랭킹 조회 (내부 공통 로직)
     */
    private List<RankingItem> getRanking(String key, int page, int size) {
        // 페이지 계산 (1-based → 0-based)
        int start = (page - 1) * size;
        int end = start + size - 1;

        // ZREVRANGE로 점수 높은 순으로 조회
        Set<TypedTuple<String>> results = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, start, end);

        if (results == null || results.isEmpty()) {
            log.debug("📊 랭킹 조회 결과 없음 - key: {}, page: {}, size: {}", key, page, size);
            return List.of();
        }

        List<RankingItem> items = new ArrayList<>();
        int rank = start + 1;  // 순위는 1부터 시작

        for (TypedTuple<String> tuple : results) {
            Long productId = Long.parseLong(tuple.getValue());
            Double score = tuple.getScore();

            items.add(RankingItem.builder()
                .rank(rank++)
                .productId(productId)
                .score(score)
                .build());
        }

        log.info("📊 랭킹 조회 완료 - key: {}, page: {}, size: {}, count: {}",
            key, page, size, items.size());

        return items;
    }

    /**
     * 랭킹 아이템 DTO
     */
    @lombok.Getter
    @lombok.Builder
    public static class RankingItem {
        private int rank;           // 순위 (1부터 시작)
        private Long productId;     // 상품 ID
        private Double score;       // 점수
    }
}
