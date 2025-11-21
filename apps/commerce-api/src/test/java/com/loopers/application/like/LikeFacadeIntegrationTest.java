package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.productlike.LikeSortType;
import com.loopers.domain.productlike.LikedProduct;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.productlike.ProductLikeRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.test.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DisplayName("LikeFacade 통합 테스트")
class LikeFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Autowired
  private LikeFacade likeFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private BrandRepository brandRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

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

      likeFacade.registerProductLike(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 사용자가 두 번 좋아요 시도해도 1개만 증가한다. (멱등성)")
    void ensuresIdempotency_whenDuplicateLikes() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Long userId = 1L;
      Long productId = product.getId();

      likeFacade.registerProductLike(userId, productId);
      likeFacade.registerProductLike(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 시 예외 발생한다")
    void throwsException_whenProductNotFound() {
      Long userId = 1L;
      Long productId = 999999L;

      assertThatThrownBy(() -> likeFacade.registerProductLike(userId, productId))
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

      likeFacade.cancelProductLike(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("같은 사용자가 두 번 좋아요 취소 시도해도 1개만 감소한다 (멱등성)")
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

      likeFacade.cancelProductLike(userId, productId);
      likeFacade.cancelProductLike(userId, productId);

      assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse();
      Product updatedProduct = productRepository.findById(productId).orElseThrow();
      assertThat(updatedProduct.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 취소 시 예외가 발생한다")
    void throwsException_whenProductNotFound() {
      Long userId = 1L;
      Long productId = 999999L;

      assertThatThrownBy(() -> likeFacade.cancelProductLike(userId, productId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
          .hasMessageContaining("상품을 찾을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("좋아요 목록 조회 시")
  class RetrieveLikedProducts {

    @Test
    @DisplayName("LATEST 정렬 - 최근 좋아요 순으로 조회한다")
    void retrievesLikedProducts_sortedByLatest() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId()));
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brand.getId()));

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, product1.getId(), LIKED_AT_2025_10_30));
      productLikeRepository.save(ProductLike.of(userId, product2.getId(),
          LIKED_AT_2025_10_30.plusSeconds(10)));

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId, LikeSortType.LATEST,
          PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(2)
          .extracting("productName", "likedAt")
          .containsExactly(
              tuple("상품2", LIKED_AT_2025_10_30.plusSeconds(10)),
              tuple("상품1", LIKED_AT_2025_10_30)
          );
    }

    @Test
    @DisplayName("PRODUCT_NAME 정렬 - 상품명 오름차순으로 조회한다")
    void retrievesLikedProducts_sortedByProductName() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product charlie = productRepository.save(
          Product.of("Charlie", Money.of(30000L), "설명C", Stock.of(30L), brand.getId()));
      Product alpha = productRepository.save(
          Product.of("Alpha", Money.of(10000L), "설명A", Stock.of(100L), brand.getId()));
      Product beta = productRepository.save(
          Product.of("Beta", Money.of(20000L), "설명B", Stock.of(50L), brand.getId()));

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, charlie.getId(), LIKED_AT_2025_10_30));
      productLikeRepository.save(
          ProductLike.of(userId, alpha.getId(), LIKED_AT_2025_10_30.plusSeconds(1)));
      productLikeRepository.save(
          ProductLike.of(userId, beta.getId(), LIKED_AT_2025_10_30.plusSeconds(2)));

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId,
          LikeSortType.PRODUCT_NAME, PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(3)
          .extracting("productName")
          .containsExactly("Alpha", "Beta", "Charlie");
    }

    @Test
    @DisplayName("PRICE_ASC 정렬 - 가격 오름차순으로 조회한다")
    void retrievesLikedProducts_sortedByPriceAsc() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product expensive = productRepository.save(
          Product.of("비싼상품", Money.of(50000L), "설명", Stock.of(10L), brand.getId()));
      Product cheap = productRepository.save(
          Product.of("저렴한상품", Money.of(10000L), "설명", Stock.of(100L), brand.getId()));
      Product medium = productRepository.save(
          Product.of("중간상품", Money.of(30000L), "설명", Stock.of(50L), brand.getId()));

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, expensive.getId(), LIKED_AT_2025_10_30));
      productLikeRepository.save(
          ProductLike.of(userId, cheap.getId(), LIKED_AT_2025_10_30.plusSeconds(1)));
      productLikeRepository.save(
          ProductLike.of(userId, medium.getId(), LIKED_AT_2025_10_30.plusSeconds(2)));

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId,
          LikeSortType.PRICE_ASC, PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(3)
          .extracting("productName", "price")
          .containsExactly(
              tuple("저렴한상품", 10000L),
              tuple("중간상품", 30000L),
              tuple("비싼상품", 50000L)
          );
    }

    @Test
    @DisplayName("PRICE_DESC 정렬 - 가격 내림차순으로 조회한다")
    void retrievesLikedProducts_sortedByPriceDesc() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product expensive = productRepository.save(
          Product.of("비싼상품", Money.of(50000L), "설명", Stock.of(10L), brand.getId()));
      Product cheap = productRepository.save(
          Product.of("저렴한상품", Money.of(10000L), "설명", Stock.of(100L), brand.getId()));
      Product medium = productRepository.save(
          Product.of("중간상품", Money.of(30000L), "설명", Stock.of(50L), brand.getId()));

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, expensive.getId(), LIKED_AT_2025_10_30));
      productLikeRepository.save(
          ProductLike.of(userId, cheap.getId(), LIKED_AT_2025_10_30.plusSeconds(1)));
      productLikeRepository.save(
          ProductLike.of(userId, medium.getId(), LIKED_AT_2025_10_30.plusSeconds(2)));

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId,
          LikeSortType.PRICE_DESC, PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(3)
          .extracting("productName", "price")
          .containsExactly(
              tuple("비싼상품", 50000L),
              tuple("중간상품", 30000L),
              tuple("저렴한상품", 10000L)
          );
    }

    @Test
    @DisplayName("좋아요한 상품이 없으면 빈 페이지를 반환한다")
    void returnsEmptyPage_whenNoLikedProducts() {
      Long userId = 1L;

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId, LikeSortType.LATEST,
          PageRequest.of(0, 10));

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("브랜드 정보가 포함된다")
    void includesBrandInformation() {
      Brand brandA = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Brand brandB = brandRepository.save(Brand.of("브랜드B", "설명B"));
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brandA.getId()));
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brandB.getId()));

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, product1.getId(), LIKED_AT_2025_10_30));
      productLikeRepository.save(
          ProductLike.of(userId, product2.getId(), LIKED_AT_2025_10_30.plusSeconds(1)));

      Page<LikedProduct> result = likeFacade.retrieveLikedProducts(userId, LikeSortType.LATEST,
          PageRequest.of(0, 10));

      assertThat(result.getContent())
          .hasSize(2)
          .extracting("brandName")
          .containsExactly("브랜드B", "브랜드A");
    }
  }
}
