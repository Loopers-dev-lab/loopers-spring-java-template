package com.loopers.infrastructure.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RankingRedisService {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long TTL_SECONDS = 172800; // 2일

    private final RedisTemplate<String, String> redisTemplate;

    private static final double VIEW_SCORE = 1.0;
    private static final double LIKE_SCORE = 2.0;
    private static final double ORDER_SCORE = 6.0;

    public List<RankingEntry> getTopProducts(LocalDate date, int offset, int limit) {
        String key = generateKey(date);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, offset + limit - 1);

        List<RankingEntry> entries = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Long productId = Long.parseLong(tuple.getValue());
                Double score = tuple.getScore();
                entries.add(new RankingEntry(productId, score));
            }
        }
        return entries;
    }

    public void incrementScoreForView(LocalDate date, Long productId) {
        incrementScoreForView(date, productId, 1);
    }

    public void incrementScoreForView(LocalDate date, Long productId, int count) {
        String key = generateKey(date);
        incrementScoreWithExpire(key, String.valueOf(productId), VIEW_SCORE * count);
    }

    public void incrementScoreForLike(LocalDate date, Long productId) {
        String key = generateKey(date);
        incrementScoreWithExpire(key, String.valueOf(productId), LIKE_SCORE);
    }

    public void incrementScoreForOrder(LocalDate date, Long productId, Long quantity) {
        String key = generateKey(date);
        incrementScoreWithExpire(key, String.valueOf(productId), ORDER_SCORE * quantity);
    }

    public Long getRankingPosition(LocalDate date, Long productId) {
        String key = generateKey(date);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(productId));
        if (rank == null) {
            return null;
        }
        return rank + 1;
    }

    public long getTotalCount(LocalDate date) {
        String key = generateKey(date);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }

    // 주간 랭킹 캐시
    public void cacheWeeklyRanking(LocalDate weekStart, List<RankingEntry> entries) {
        String key = "ranking:weekly:" + weekStart.format(DATE_FORMATTER);
        redisTemplate.delete(key);
        for (RankingEntry entry : entries) {
            redisTemplate.opsForZSet().add(key, String.valueOf(entry.productId()), entry.score());
        }
        redisTemplate.expire(key, 25, TimeUnit.HOURS); // 25시간 TTL
    }

    public List<RankingEntry> getWeeklyRankingCache(LocalDate weekStart, int offset, int limit) {
        String key = "ranking:weekly:" + weekStart.format(DATE_FORMATTER);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, offset + limit - 1);

        List<RankingEntry> entries = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                entries.add(new RankingEntry(Long.parseLong(tuple.getValue()), tuple.getScore()));
            }
        }
        return entries;
    }

    public long getWeeklyRankingCacheCount(LocalDate weekStart) {
        String key = "ranking:weekly:" + weekStart.format(DATE_FORMATTER);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }

    // 월간 랭킹 캐시
    public void cacheMonthlyRanking(LocalDate monthStart, List<RankingEntry> entries) {
        String key = "ranking:monthly:" + monthStart.format(DATE_FORMATTER);
        redisTemplate.delete(key);
        for (RankingEntry entry : entries) {
            redisTemplate.opsForZSet().add(key, String.valueOf(entry.productId()), entry.score());
        }
        redisTemplate.expire(key, 25, TimeUnit.HOURS);
    }

    public List<RankingEntry> getMonthlyRankingCache(LocalDate monthStart, int offset, int limit) {
        String key = "ranking:monthly:" + monthStart.format(DATE_FORMATTER);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, offset + limit - 1);

        List<RankingEntry> entries = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                entries.add(new RankingEntry(Long.parseLong(tuple.getValue()), tuple.getScore()));
            }
        }
        return entries;
    }

    public long getMonthlyRankingCacheCount(LocalDate monthStart) {
        String key = "ranking:monthly:" + monthStart.format(DATE_FORMATTER);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;
    }

    private void incrementScoreWithExpire(String key, String member, double score) {
        boolean isNewKey = Boolean.FALSE.equals(redisTemplate.hasKey(key));
        redisTemplate.opsForZSet().incrementScore(key, member, score);
        if (isNewKey) {
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private String generateKey(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
