package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingRepository;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RankingRedisRepository implements RankingRepository {

  private final StringRedisTemplate redisTemplate;
  private final RankingRedisProperties properties;

  @Override
  public void incrementScores(Map<String, Map<Long, Double>> bucketToScores) {
    Set<String> keysToSetTtl = bucketToScores.keySet();

    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
      bucketToScores.forEach((key, scores) -> incrementBucket(connection, key, scores));
      return null;
    });

    keysToSetTtl.forEach(key -> setTtlIfAbsent(key, properties.getTtl()));
  }

  private void incrementBucket(RedisConnection connection, String key, Map<Long, Double> scores) {
    byte[] keyBytes = key.getBytes();
    scores.forEach((productId, score) ->
        connection.zSetCommands().zIncrBy(keyBytes, score, productId.toString().getBytes())
    );
  }

  @Override
  public void setTtlIfAbsent(String key, Duration ttl) {
    Long currentTtl = redisTemplate.getExpire(key);
    if (currentTtl == -1) {
      redisTemplate.expire(key, ttl);
    }
  }
}
