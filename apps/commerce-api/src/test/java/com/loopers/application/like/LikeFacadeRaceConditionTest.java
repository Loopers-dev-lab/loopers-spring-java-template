package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.utils.DatabaseCleanUp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("LikeFacade 동시성 테스트")
public class LikeFacadeRaceConditionTest extends IntegrationTestSupport {

  @Autowired
  private LikeFacade likeFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("좋아요 추가 동시성")
  class ConcurrentLike {

    @Test
    @DisplayName("10명이 동시에 같은 상품에 좋아요 시 카운트가 정확히 10으로 증가한다")
    void countsCorrectly() {
      // given
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), 1L)
      );

      // when
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (int i = 1; i <= 10; i++) {
        Long userId = (long) i;
        futures.add(asyncExecute(() -> likeFacade.registerProductLike(userId, product.getId())));
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // then
      Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
      long likeCount = updatedProduct.getLikeCount();
      assertThat(likeCount).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은 유저가 동시에 5번 좋아요 시 중복 방지되어 1개만 등록된다")
    void preventsDuplicate() {
      // given
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), 1L)
      );
      Long userId = 1L;

      // when
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        futures.add(asyncExecute(() -> likeFacade.registerProductLike(userId, product.getId())));
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // then
      Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
      long likeCount = updatedProduct.getLikeCount();
      assertThat(likeCount).isEqualTo(1L);

      List<ProductLike> likes = productLikeRepository.findByUserIdAndProductIdIn(userId, List.of(product.getId()));
      assertThat(likes).hasSize(1);
    }

  }

  @Nested
  @DisplayName("좋아요 취소 동시성")
  class ConcurrentUnlike {

    @Test
    @DisplayName("10명이 동시에 좋아요 취소 시 카운트가 정확히 0으로 감소한다")
    void countsCorrectly() {
      // given
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), 1L)
      );

      for (int i = 1; i <= 10; i++) {
        Long userId = (long) i;
        likeFacade.registerProductLike(userId, product.getId());
      }

      // when
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (int i = 1; i <= 10; i++) {
        Long userId = (long) i;
        futures.add(asyncExecute(() -> likeFacade.cancelProductLike(userId, product.getId())));
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // then
      Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
      long likeCount = updatedProduct.getLikeCount();
      assertThat(likeCount).isZero();
    }
  }

  private CompletableFuture<Void> asyncExecute(Runnable task) {
    return CompletableFuture.runAsync(() -> {
      try {
        task.run();
      } catch (Exception e) {
        // 동시성 테스트에서는 예외 무시
      }
    });
  }
}
