package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;

    public Set<ZSetOperations.TypedTuple<String>> getDailyMembers(String yyyymmdd, long start, long end) {
        String key = "ranking:all:" + yyyymmdd;
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    public Integer getDailyRank(String yyyymmdd, Long productId) {
        String key = "ranking:all:" + yyyymmdd;
        Long idx = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(productId));
        if (idx == null) return null;
        long oneBased = idx + 1;
        if (oneBased > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) oneBased;
    }
}


