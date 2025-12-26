package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.loopers.support.util.RedisUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

  private final RedisTemplate<String, String> redisTemplate;

  private static final double VIEW_WEIGHT = 0.1;
  private static final double LIKE_WEIGHT = 0.2;
  private static final double ORDER_WEIGHT = 0.6;
  private static final long TTL_DAYS = 2;

  /**
   * 상품 조회 점수 추가
   *
   * @param productId 상품 ID
   */
  public void addViewScore(Long productId) {
    String rankingKey = getTodayRankingKey();
    double score = VIEW_WEIGHT * 1.0; // Weight = 0.1, Score = 1

    try {
      redisTemplate.opsForZSet().incrementScore(rankingKey, productId.toString(), score);
      RedisUtils.setTtlInDaysIfNeeded(redisTemplate, rankingKey, TTL_DAYS);

      log.debug("Added view score for product: productId={}, score={}", productId, score);
    } catch (Exception e) {
      log.error("Failed to add view score: productId={}", productId, e);
    }
  }

  /**
   * 상품 좋아요 점수 추가
   *
   * @param productId 상품 ID
   */
  public void addLikeScore(Long productId) {
    String rankingKey = getTodayRankingKey();
    double score = LIKE_WEIGHT * 1.0; // Weight = 0.2, Score = 1

    try {
      redisTemplate.opsForZSet().incrementScore(rankingKey, productId.toString(), score);
      RedisUtils.setTtlInDaysIfNeeded(redisTemplate, rankingKey, TTL_DAYS);

      log.debug("Added like score for product: productId={}, score={}", productId, score);
    } catch (Exception e) {
      log.error("Failed to add like score: productId={}", productId, e);
    }
  }

  /**
   * 상품 좋아요 점수 차감
   *
   * @param productId 상품 ID
   */
  public void subtractLikeScore(Long productId) {
    String rankingKey = getTodayRankingKey();
    double score = -(LIKE_WEIGHT * 1.0); // 좋아요 취소시 점수 차감

    try {
      redisTemplate.opsForZSet().incrementScore(rankingKey, productId.toString(), score);
      RedisUtils.setTtlInDaysIfNeeded(redisTemplate, rankingKey, TTL_DAYS);

      log.debug("Removed like score for product: productId={}, score={}", productId, score);
    } catch (Exception e) {
      log.error("Failed to remove like score: productId={}", productId, e);
    }
  }

  /**
   * 상품 주문 점수 추가
   *
   * @param productId 상품 ID
   * @param price     상품 가격
   * @param quantity  주문 수량
   */
  public void addSalesScore(Long productId, Long price, Long quantity) {
    String rankingKey = getTodayRankingKey();
    double orderScore = price * quantity; // Score = price * amount
    double score = ORDER_WEIGHT * orderScore; // Weight = 0.6

    try {
      redisTemplate.opsForZSet().incrementScore(rankingKey, productId.toString(), score);
      RedisUtils.setTtlInDaysIfNeeded(redisTemplate, rankingKey, TTL_DAYS);

      log.debug("Added order score for product: productId={}, price={}, quantity={}, score={}",
          productId, price, quantity, score);
    } catch (Exception e) {
      log.error("Failed to add order score: productId={}", productId, e);
    }
  }

  /**
   * 일별 랭킹 키 생성
   *
   * @return ranking:all:{yyyyMMdd}
   */
  private String getTodayRankingKey() {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return String.format("ranking:all:%s", today);
  }

}
