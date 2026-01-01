package com.loopers.application.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingRedisReader {
    private final StringRedisTemplate redisTemplate;

    public RankingInfo getDailyRanking(String date, Long productId) {
        String key = "ranking:all:" + date;
        String member = String.valueOf(productId);

        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        byte[] keyBytes = serializer.serialize(key);
        byte[] memberBytes = serializer.serialize(member);

        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.zScore(keyBytes, memberBytes);   // -> Double (or null)
            connection.zRevRank(keyBytes, memberBytes); // -> Long   (or null) 0-base
            connection.zCard(keyBytes);                 // -> Long
            return null;
        });

        Double score = (Double) results.get(0);
        Long revRank0 = (Long) results.get(1);
        Long total = (Long) results.get(2);

        Integer rank = (revRank0 == null) ? null : Math.toIntExact(revRank0 + 1);

        return new RankingInfo(date, score, rank, total);
    }
}
