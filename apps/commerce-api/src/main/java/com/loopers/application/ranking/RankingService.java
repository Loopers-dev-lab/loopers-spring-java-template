package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Service
public class RankingService {

  private final RedisTemplate<String, String> redisTemplate;

  public Integer getProductRank(Long productId) {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String rankingKey = "ranking:all:" + date;
    Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, productId.toString());

    // rank가 null이면 랭킹에 없는 상품
    return rank != null ? rank.intValue() + 1 : null; // 0부터 시작하므로 +1
  }

  /**
   * 특정 날짜의 전체 랭킹에서 상품의 순위를 조회
   *
   * @param date      조회할 날짜 (yyyyMMdd 형식)
   * @param productId 상품 ID
   * @return 랭킹 순위 (1부터 시작), 랭킹에 없으면 null
   */
  public Integer getProductRank(String date, Long productId) {
    String rankingKey = "ranking:all:" + date;
    Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, productId.toString());

    // rank가 null이면 랭킹에 없는 상품
    return rank != null ? rank.intValue() + 1 : null; // 0부터 시작하므로 +1
  }

  /**
   * 특정 날짜의 전체 랭킹에서 상품의 점수를 조회
   *
   * @param date      조회할 날짜 (yyyyMMdd 형식)
   * @param productId 상품 ID
   * @return 랭킹 점수, 랭킹에 없으면 null
   */
  public Double getProductScore(String date, Long productId) {
    String rankingKey = "ranking:all:" + date;
    return redisTemplate.opsForZSet().score(rankingKey, productId.toString());
  }

  /**
   * 특정 날짜의 전체 랭킹 총 개수 조회
   *
   * @param date 조회할 날짜 (yyyyMMdd 형식)
   * @return 랭킹에 있는 총 상품 개수
   */
  public Long getTotalRankingCount(String date) {
    String rankingKey = "ranking:all:" + date;
    return redisTemplate.opsForZSet().zCard(rankingKey);
  }
}
