package com.loopers.application.product;

import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.application.like.LikeFacade;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Autowired
  private ProductFacade productFacade;

  @Autowired
  private LikeFacade likeFacade;

  @Autowired
  private BrandRepository brandRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ProductLikeRepository productLikeRepository;

  @Nested
  @DisplayName("상품 목록 조회 시")
  class GetProducts {

    @Test
    @DisplayName("브랜드 필터 없이 조회하면 전체 상품이 반환된다")
    void getsAllProducts_withoutBrandFilter() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .hasSize(2)
          .extracting("productName", "price")
          .containsExactlyInAnyOrder(
              tuple("상품1", 10000L),
              tuple("상품2", 20000L)
          );
    }

    @Test
    @DisplayName("특정 브랜드로 필터링하면 해당 브랜드의 상품만 반환된다")
    void getsProducts_withBrandFilter() {
      Brand brand1 = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Brand brand2 = brandRepository.save(Brand.of("브랜드B", "설명B"));

      productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand1.getId())
      );
      productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brand1.getId())
      );
      productRepository.save(
          Product.of("상품3", Money.of(30000L), "설명3", Stock.of(30L), brand2.getId())
      );

      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(brand1.getId(), null, pageable);

      assertThat(result.getContent())
          .hasSize(2)
          .extracting("productName", "price")
          .containsExactlyInAnyOrder(
              tuple("상품1", 10000L),
              tuple("상품2", 20000L)
          );
    }

    @Test
    @DisplayName("사용자가 좋아요한 상품은 isLiked가 true로 반환된다")
    void includesLikeStatus_whenUserLikesProduct() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(50L), brand.getId())
      );

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, product1.getId(), LIKED_AT_2025_10_30)
      );

      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, userId, pageable);

      assertThat(result.getContent())
          .extracting("productName", "liked")
          .containsExactlyInAnyOrder(
              tuple("상품1", true),
              tuple("상품2", false)
          );
    }

    @Test
    @DisplayName("userId 없이 조회 시 모든 상품의 isLiked가 false로 반환된다")
    void returnsAllLikedFalse_whenUserIdIsNull() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .extracting("liked")
          .containsOnly(false);
    }

    @Test
    @DisplayName("페이지네이션을 적용하면 페이지 크기만큼만 상품이 반환된다")
    void supportsPagination() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      for (int i = 1; i <= 5; i++) {
        productRepository.save(
            Product.of("상품" + i, Money.of(10000L), "설명" + i, Stock.of(100L), brand.getId())
        );
      }

      Pageable pageable = PageRequest.of(0, 2);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertAll(
          () -> assertThat(result.getContent()).hasSize(2),
          () -> assertThat(result.getTotalElements()).isEqualTo(5L),
          () -> assertThat(result.getTotalPages()).isEqualTo(3)
      );
    }

    @Test
    @DisplayName("상품이 하나도 없을 때 빈 페이지를 반환한다")
    void returnsEmptyPage_whenNoProducts() {
      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 브랜드로 필터링 시 빈 페이지를 반환한다")
    void returnsEmptyPage_whenBrandHasNoProducts() {
      Brand brand = brandRepository.save(Brand.of("빈브랜드", "상품없음"));

      Pageable pageable = PageRequest.of(0, 20);
      Page<ProductDetail> result = productFacade.searchProductDetails(brand.getId(), null, pageable);

      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("마지막 페이지를 조회하면 남은 상품만 반환된다")
    void getsLastPage() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      for (int i = 1; i <= 5; i++) {
        productRepository.save(
            Product.of("상품" + i, Money.of(10000L), "설명", Stock.of(100L), brand.getId())
        );
      }

      Pageable pageable = PageRequest.of(2, 2);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("범위를 벗어난 페이지 조회 시 빈 페이지를 반환한다")
    void returnsEmptyPage_whenPageExceedsRange() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(10, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 사용자가 좋아요한 상품의 총 좋아요 수가 정확하다")
    void showsCorrectLikeCount_whenMultipleUsersLike() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("인기상품", Money.of(10000L), "설명", Stock.of(100L), brand.getId())
      );

      for (long userId = 1L; userId <= 3L; userId++) {
        likeFacade.registerProductLike(userId, product.getId());
      }

      Pageable pageable = PageRequest.of(0, 20);
      Page<ProductDetail> result = productFacade.searchProductDetails(null, 1L, pageable);

      assertThat(result.getContent())
          .hasSize(1)
          .first()
          .extracting("likeCount", "liked")
          .containsExactly(3L, true);
    }

    @Test
    @DisplayName("좋아요가 0인 상품도 정상 조회된다")
    void getsProduct_withZeroLikes() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("인기없는상품", Money.of(10000L), "설명", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20);
      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .hasSize(1)
          .first()
          .extracting("likeCount", "liked")
          .containsExactly(0L, false);
    }

    @Test
    @DisplayName("응답에 브랜드 정보가 정확히 포함된다")
    void includesBrandSummary_inResponse() {
      Brand brand = brandRepository.save(Brand.of("나이키", "설명"));
      productRepository.save(
          Product.of("에어맥스", Money.of(150000L), "설명", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20);
      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .hasSize(1)
          .first()
          .extracting("brandId", "brandName")
          .containsExactly(brand.getId(), "나이키");
    }

    @Test
    @DisplayName("존재하지 않는 브랜드 ID로 필터링 시 빈 결과를 반환한다")
    void returnsEmpty_whenBrandIdDoesNotExist() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(100L), brand.getId())
      );

      Long nonExistentBrandId = 999L;
      Pageable pageable = PageRequest.of(0, 20);

      Page<ProductDetail> result = productFacade.searchProductDetails(nonExistentBrandId, null, pageable);

      assertThat(result.getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("상품 상세 조회 시")
  class GetProduct {

    @Test
    @DisplayName("상품을 조회하면 상품 정보와 브랜드 정보가 반환된다")
    void returnsProductWithBrand() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "상세설명", Stock.of(100L), brand.getId())
      );

      ProductDetail result = productFacade.retrieveProductDetail(product.getId(), null);

      assertThat(result)
          .extracting("productName", "price", "description", "stock", "brandName")
          .containsExactly("상품1", 10000L, "상세설명", 100L, "브랜드A");
    }

    @Test
    @DisplayName("좋아요한 상품 조회 시 isLiked가 true로 반환된다")
    void returnsIsLikedTrue_whenUserLikesProduct() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );

      Long userId = 1L;
      productLikeRepository.save(
          ProductLike.of(userId, product.getId(), LIKED_AT_2025_10_30)
      );

      ProductDetail result = productFacade.retrieveProductDetail(product.getId(), userId);

      assertThat(result.liked()).isTrue();
    }

    @Test
    @DisplayName("좋아요 안 한 상품 조회 시 isLiked가 false로 반환된다")
    void returnsIsLikedFalse_whenUserDoesNotLikeProduct() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );

      Long userId = 1L;

      ProductDetail result = productFacade.retrieveProductDetail(product.getId(), userId);

      assertThat(result.liked()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID 조회 시 404 예외가 발생한다")
    void throwsNotFoundException_whenProductDoesNotExist() {
      Long nonExistentProductId = 999L;

      assertThatThrownBy(() ->
          productFacade.retrieveProductDetail(nonExistentProductId, null)
      )
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("브랜드가 존재하지 않으면 404 예외가 발생한다")
    void throwsNotFoundException_whenBrandDoesNotExist() {
      Long nonExistentBrandId = 999L;
      Product product = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), nonExistentBrandId)
      );
      Long productId = product.getId();

      assertThatThrownBy(() ->
          productFacade.retrieveProductDetail(productId, null)
      )
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("상품 정렬 시")
  class SortProducts {

    @Test
    @DisplayName("가격 오름차순으로 정렬하면 저렴한 순서로 상품이 반환된다")
    void sortsByPriceAscending() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품C", Money.of(30000L), "설명C", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품A", Money.of(10000L), "설명A", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품B", Money.of(20000L), "설명B", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20, Sort.by("price").ascending());

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .extracting("productName", "price")
          .containsExactly(
              tuple("상품A", 10000L),
              tuple("상품B", 20000L),
              tuple("상품C", 30000L)
          );
    }

    @Test
    @DisplayName("좋아요순으로 정렬하면 좋아요가 많은 순서로 상품이 반환된다")
    void sortsByLikesDescending() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
      );
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(20000L), "설명2", Stock.of(100L), brand.getId())
      );
      Product product3 = productRepository.save(
          Product.of("상품3", Money.of(30000L), "설명3", Stock.of(100L), brand.getId())
      );

      for (long userId = 1L; userId <= 5L; userId++) {
        likeFacade.registerProductLike(userId, product1.getId());
      }

      for (long userId = 1L; userId <= 10L; userId++) {
        likeFacade.registerProductLike(userId, product2.getId());
      }

      for (long userId = 1L; userId <= 2L; userId++) {
        likeFacade.registerProductLike(userId, product3.getId());
      }

      Pageable pageable = PageRequest.of(0, 20, Sort.by("likeCount").descending());

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .extracting("productName", "likeCount")
          .containsExactly(
              tuple("상품2", 10L),
              tuple("상품1", 5L),
              tuple("상품3", 2L)
          );
    }

    @Test
    @DisplayName("최신순으로 정렬하면 최근 생성된 순서로 상품이 반환된다")
    void sortsByLatest() {
      Brand brand = brandRepository.save(Brand.of("브랜드A", "설명A"));
      productRepository.save(
          Product.of("상품A", Money.of(10000L), "설명A", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품B", Money.of(20000L), "설명B", Stock.of(100L), brand.getId())
      );
      productRepository.save(
          Product.of("상품C", Money.of(30000L), "설명C", Stock.of(100L), brand.getId())
      );

      Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

      Page<ProductDetail> result = productFacade.searchProductDetails(null, null, pageable);

      assertThat(result.getContent())
          .extracting("productName", "price")
          .containsExactly(
              tuple("상품C", 30000L),
              tuple("상품B", 20000L),
              tuple("상품A", 10000L)
          );
    }
  }
}
