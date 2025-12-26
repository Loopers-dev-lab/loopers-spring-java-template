package com.loopers.application.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import com.loopers.support.test.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RankingFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDateTime LIKED_AT_2025_12_24 = LocalDateTime.of(2025, 12, 24, 10, 0, 0);
  private static final LocalDate DATE_2025_12_24 = LocalDate.of(2025, 12, 24);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  @Autowired
  private RankingFacade rankingFacade;

  @Autowired
  private BrandRepository brandRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @AfterEach
  void cleanUpRedis() {
    String key = "ranking:all:" + DATE_2025_12_24.format(DATE_FORMATTER);
    redisTemplate.delete(key);
  }

  @Nested
  @DisplayName("일간 랭킹 조회 시")
  class GetDailyRanking {

    @Test
    @DisplayName("랭킹 상품에 Product, Brand, Like 정보가 조합되어 반환된다")
    void shouldReturnAggregatedRankingData() {
      Brand brand = brandRepository.save(Brand.of("테스트브랜드", "설명"));
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brand.getId())
      );

      String key = "ranking:all:" + DATE_2025_12_24.format(DATE_FORMATTER);
      redisTemplate.opsForZSet().add(key, product1.getId().toString(), 10.0);
      redisTemplate.opsForZSet().add(key, product2.getId().toString(), 20.0);

      Long userId = 1L;
      productLikeRepository.save(ProductLike.of(userId, product2.getId(), LIKED_AT_2025_12_24));

      RankingResult result = rankingFacade.getDailyRanking(DATE_2025_12_24, 0, 10, userId);

      assertThat(result.rankings())
          .hasSize(2)
          .extracting(
              RankingItemResult::rank,
              RankingItemResult::productName,
              RankingItemResult::brandName,
              RankingItemResult::liked
          )
          .containsExactly(
              tuple(1, "상품2", "테스트브랜드", true),
              tuple(2, "상품1", "테스트브랜드", false)
          );
    }

    @Test
    @DisplayName("랭킹 데이터가 없으면 빈 결과를 반환한다")
    void shouldReturnEmptyResult_whenNoRankingData() {
      RankingResult result = rankingFacade.getDailyRanking(DATE_2025_12_24, 0, 10, null);

      assertThat(result.rankings()).isEmpty();
      assertThat(result.date()).isEqualTo(DATE_2025_12_24);
    }

    @Test
    @DisplayName("userId가 null이면 liked는 모두 false로 반환된다")
    void shouldReturnAllLikedFalse_whenUserIdIsNull() {
      Brand brand = brandRepository.save(Brand.of("테스트브랜드", "설명"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );

      String key = "ranking:all:" + DATE_2025_12_24.format(DATE_FORMATTER);
      redisTemplate.opsForZSet().add(key, product.getId().toString(), 10.0);

      RankingResult result = rankingFacade.getDailyRanking(DATE_2025_12_24, 0, 10, null);

      assertThat(result.rankings())
          .extracting(RankingItemResult::liked)
          .containsOnly(false);
    }

    @Test
    @DisplayName("삭제된 상품은 결과에서 제외된다")
    void shouldExcludeDeletedProducts() {
      Brand brand = brandRepository.save(Brand.of("테스트브랜드", "설명"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );

      String key = "ranking:all:" + DATE_2025_12_24.format(DATE_FORMATTER);
      redisTemplate.opsForZSet().add(key, product.getId().toString(), 10.0);
      redisTemplate.opsForZSet().add(key, "99999", 20.0);

      RankingResult result = rankingFacade.getDailyRanking(DATE_2025_12_24, 0, 10, null);

      assertThat(result.rankings())
          .hasSize(1)
          .extracting(RankingItemResult::productName)
          .containsOnly("상품1");
    }
  }
}
