package com.loopers.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RankingRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public void increaseScore(String key, String member, double score, Duration ttl) {
        if (score == 0d) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(key, member, score);
        redisTemplate.expire(key, ttl);
    }

    public List<RankingRow> fetchPage(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankingRow> rows = new ArrayList<>();
        long rank = start + 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                rank++;
                continue;
            }
            Double score = tuple.getScore();
            rows.add(new RankingRow(rank, Long.valueOf(tuple.getValue()), score != null ? score : 0d));
            rank++;
        }
        return rows;
    }

    public long count(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0L;
    }

    public Long findRank(String key, Long productId) {
        if (productId == null) {
            return null;
        }
        Long rank = redisTemplate.opsForZSet().reverseRank(key, productId.toString());
        return rank == null ? null : rank + 1;
    }

    public void scaleFrom(String sourceKey, String targetKey, double weight, Duration ttl) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(sourceKey, 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            Double score = tuple.getScore();
            if (score == null || score == 0d) {
                continue;
            }
            redisTemplate.opsForZSet().add(targetKey, tuple.getValue(), score * weight);
        }
        redisTemplate.expire(targetKey, ttl);
    }
}
