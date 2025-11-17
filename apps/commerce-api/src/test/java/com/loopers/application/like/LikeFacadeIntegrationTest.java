package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("LikeFacade 통합 테스트")
class LikeFacadeIntegrationTest {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Autowired
  private LikeFacade likeFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private BrandRepository brandRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("좋아요 등록 시")
  class LikeProduct {

    @Test
    @DisplayName("좋아요 등록 성공하고 상품의 좋아요 수가 증가한다")
    void likesProduct_andIncreasesLikeCount() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Long userId = 1L;
      Long productId = product.getId();

      likeFacade.likeProduct(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 사용자가 두 번 좋아요 시도해도 1개만 증가 (멱등성)")
    void ensuresIdempotency_whenDuplicateLikes() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Long userId = 1L;
      Long productId = product.getId();

      likeFacade.likeProduct(userId, productId);
      likeFacade.likeProduct(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 시 예외 발생")
    void throwsException_whenProductNotFound() {
      Long userId = 1L;
      Long productId = 999999L;

      assertThatThrownBy(() -> likeFacade.likeProduct(userId, productId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
          .hasMessageContaining("상품을 찾을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("좋아요 취소 시")
  class UnlikeProduct {

    @Test
    @DisplayName("좋아요 취소 성공하고 상품의 좋아요 수가 감소한다")
    void unlikesProduct_andDecreasesLikeCount() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Long userId = 1L;
      Long productId = product.getId();

      productLikeRepository.save(ProductLike.of(userId, productId, LIKED_AT_2025_10_30));
      product.increaseLikeCount();
      productRepository.save(product);

      likeFacade.unlikeProduct(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("같은 사용자가 두 번 좋아요 취소 시도해도 1개만 감소 (멱등성)")
    void ensuresIdempotency_whenDuplicateUnlikes() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Long userId = 1L;
      Long productId = product.getId();

      productLikeRepository.save(ProductLike.of(userId, productId, LIKED_AT_2025_10_30));
      product.increaseLikeCount();
      productRepository.save(product);

      likeFacade.unlikeProduct(userId, productId);
      likeFacade.unlikeProduct(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 취소 시 예외 발생")
    void throwsException_whenProductNotFound() {
      Long userId = 1L;
      Long productId = 999999L;

      assertThatThrownBy(() -> likeFacade.unlikeProduct(userId, productId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
          .hasMessageContaining("상품을 찾을 수 없습니다.");
    }
  }
}
