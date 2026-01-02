package com.loopers.domain.rank;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingService {

  private final RedisTemplate<String, String> redisTemplate;
  private final RankingKeyGenerator rankingKeyGenerator;
  private static final double VIEW_WEIGHT = 0.1;
  private static final double LIKE_WEIGHT = 0.2;
  private static final double ORDER_WEIGHT = 0.6;

  public void addScore(Long productId, double baseScore, double weight, LocalDateTime dateTime) {
    String dateKey = rankingKeyGenerator.generateDailyKey(dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    double finalScore = baseScore * weight;

    redisTemplate.opsForZSet().incrementScore(dateKey, productId.toString(), finalScore);
    redisTemplate.expire(dateKey, 2, TimeUnit.DAYS);
  }

  public void addOrderScoresBatch(Map<String, Map<Long, Double>> updates) {
    updates.forEach((dateKey, productScores) -> {
      productScores.forEach((productId, score) -> {
        redisTemplate.opsForZSet().incrementScore(dateKey, productId.toString(), score);
      });
      redisTemplate.expire(dateKey, 2, TimeUnit.DAYS);
    });
  }

  public void carryOverRanking(String sourceDate, String targetDate, double weight) {
    String sourceKey = rankingKeyGenerator.generateDailyKey(sourceDate);
    String targetKey = rankingKeyGenerator.generateDailyKey(targetDate);

    redisTemplate.opsForZSet().unionAndStore(sourceKey, List.of(), targetKey,
        Aggregate.SUM, Weights.of(weight));

    redisTemplate.expire(targetKey, 2, TimeUnit.DAYS);
  }
}
