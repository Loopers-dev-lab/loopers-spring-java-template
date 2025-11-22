package com.loopers.domain.productlike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.test.IntegrationTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("ProductLikeService 통합 테스트")
class ProductLikeServiceIntegrationTest extends IntegrationTestSupport {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-10-30T00:00:00Z"), ZoneId.systemDefault());
  private static final LocalDateTime FIXED_LIKED_AT = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

  @Autowired
  private ProductLikeService productLikeService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

  @MockitoBean
  private Clock clock;

  @BeforeEach
  void setUpClock() {
    Mockito.when(clock.instant()).thenReturn(FIXED_CLOCK.instant());
    Mockito.when(clock.getZone()).thenReturn(FIXED_CLOCK.getZone());
  }

  private Product saveProduct(String name, long price, Long brandId) {
    return productRepository.save(
        Product.of(name, Money.of(price), name + " 설명", Stock.of(10L), brandId)
    );
  }

  @Nested
  @DisplayName("좋아요 생성")
  class CreateLike {

    @Test
    @DisplayName("유효한 사용자와 상품이면 좋아요가 생성된다.")
    void createsLike_whenValid() {
      long brandId = 1L;
      Product product = saveProduct("상품1", 10000L, brandId);

      productLikeService.createLike(brandId, product.getId());

      List<ProductLike> likes = productLikeRepository.findByUserIdAndProductIdIn(brandId, List.of(brandId));
      assertThat(likes).hasSize(1)
          .element(0)
          .extracting(ProductLike::getUserId, ProductLike::getProductId, ProductLike::getLikedAt)
          .containsExactly(1L, product.getId(), FIXED_LIKED_AT);
    }

    @Test
    @DisplayName("userId가 null이면 예외가 발생한다")
    void throwsWhenUserIdNull() {
      assertThatThrownBy(() -> productLikeService.createLike(null, 1L))
          .isInstanceOf(CoreException.class)
          .hasMessage("사용자는 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("productId가 null이면 예외가 발생한다")
    void throwsWhenProductIdNull() {
      assertThatThrownBy(() -> productLikeService.createLike(1L, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품은 비어있을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("좋아요 상태 조회")
  class FindLikeStatus {

    @Test
    @DisplayName("사용자가 특정 상품을 좋아요 했으면 true를 반환한다")
    void returnsTrueWhenLiked() {
      long brandId = 1L;
      Product product = saveProduct("상품1", 10000L, brandId);
      productLikeRepository.save(ProductLike.of(1L, product.getId(), FIXED_LIKED_AT));

      boolean result = productLikeService.isLiked(1L, product.getId());

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("좋아요하지 않았다면 false를 반환한다")
    void returnsFalseWhenNotLiked() {
      long brandId = 1L;
      Product product = saveProduct("상품1", 10000L, brandId);

      boolean result = productLikeService.isLiked(1L, product.getId());

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("좋아요 목록 조회")
  class FindLikedProducts {

    @Test
    @DisplayName("사용자가 좋아요한 상품 목록을 LATEST 순으로 조회한다")
    void returnsLikedProducts_sortedByLatest() {
      long brandId = 1L;
      Product product1 = saveProduct("상품1", 10000L, brandId);
      Product product2 = saveProduct("상품2", 20000L, brandId);
      productLikeRepository.save(ProductLike.of(1L, product1.getId(), FIXED_LIKED_AT));
      productLikeRepository.save(
          ProductLike.of(1L, product2.getId(), FIXED_LIKED_AT.plusSeconds(10)));

      Page<LikedProduct> result = productLikeService.findLikedProducts(1L, LikeSortType.LATEST,
          PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(2)
          .extracting(LikedProduct::productId, LikedProduct::likedAt)
          .containsExactly(
              org.assertj.core.groups.Tuple.tuple(product2.getId(),
                  FIXED_LIKED_AT.plusSeconds(10)),
              org.assertj.core.groups.Tuple.tuple(product1.getId(), FIXED_LIKED_AT)
          );
    }

    @Test
    @DisplayName("좋아요한 상품이 없으면 빈 페이지를 반환한다")
    void returnsEmptyPage_whenNoLikes() {
      Page<LikedProduct> result = productLikeService.findLikedProducts(1L, LikeSortType.LATEST,
          PageRequest.of(0, 10));

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("페이지네이션이 동작한다")
    void supportsPagination() {
      long brandId = 1L;
      for (int i = 1; i <= 5; i++) {
        Product product = saveProduct("상품" + i, 10000L * i, brandId);
        productLikeRepository.save(
            ProductLike.of(1L, product.getId(), FIXED_LIKED_AT.plusSeconds(i)));
      }

      Page<LikedProduct> firstPage = productLikeService.findLikedProducts(1L,
          LikeSortType.LATEST, PageRequest.of(0, 2));
      Page<LikedProduct> secondPage = productLikeService.findLikedProducts(1L,
          LikeSortType.LATEST, PageRequest.of(1, 2));

      assertThat(firstPage.getContent()).hasSize(2);
      assertThat(firstPage.getTotalElements()).isEqualTo(5);
      assertThat(secondPage.getContent()).hasSize(2);
      assertThat(secondPage.getTotalElements()).isEqualTo(5);
    }
  }

}
