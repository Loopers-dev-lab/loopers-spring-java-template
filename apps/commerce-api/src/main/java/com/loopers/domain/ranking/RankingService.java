package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 랭킹 조회 서비스 (commerce-api용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Top-N 랭킹 조회 (점수 포함)
     */
    public List<RankingEntry> getTopNWithScores(LocalDate date, int n) {
        String key = RankingKey.daily(date);

        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, n - 1);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            return convertToRankingEntries(tuples);
        } catch (Exception e) {
            log.error("Top-N 랭킹 조회 실패: key={}, n={}", key, n, e);
            return Collections.emptyList();
        }
    }

    /**
     * 페이지네이션 랭킹 조회
     */
    public List<RankingEntry> getRankingPage(LocalDate date, int page, int size) {
        String key = RankingKey.daily(date);
        long start = (long) page * size;
        long end = start + size - 1;

        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, start, end);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            return convertToRankingEntries(tuples);
        } catch (Exception e) {
            log.error("랭킹 페이지 조회 실패: key={}, page={}, size={}", key, page, size, e);
            return Collections.emptyList();
        }
    }

    /**
     * 특정 상품의 순위 조회
     *
     * @return 순위 (1부터 시작), 랭킹에 없으면 null
     */
    public Long getRank(Long productId, LocalDate date) {
        String key = RankingKey.daily(date);
        String member = productId.toString();

        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(key, member);
            return rank != null ? rank + 1 : null;
        } catch (Exception e) {
            log.error("순위 조회 실패: key={}, productId={}", key, productId, e);
            return null;
        }
    }

    /**
     * 특정 상품의 점수 조회
     */
    public Double getScore(Long productId, LocalDate date) {
        String key = RankingKey.daily(date);
        String member = productId.toString();

        try {
            return redisTemplate.opsForZSet().score(key, member);
        } catch (Exception e) {
            log.error("점수 조회 실패: key={}, productId={}", key, productId, e);
            return null;
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

    private List<RankingEntry> convertToRankingEntries(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankingEntry> entries = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                entries.add(new RankingEntry(
                        Long.parseLong(tuple.getValue()),
                        tuple.getScore()
                ));
            }
        }
        return entries;
    }
}
