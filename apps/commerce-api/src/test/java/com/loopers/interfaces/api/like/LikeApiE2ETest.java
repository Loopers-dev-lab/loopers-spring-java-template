package com.loopers.interfaces.api.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.stock.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.productlike.ProductLikeJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeDto.LikedProductListResponse;
import com.loopers.interfaces.api.like.LikeDto.LikedProductResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeApiE2ETest {

  private static final String BASE_URL = "/api/v1/like/products";

  private final TestRestTemplate restTemplate;
  private final BrandJpaRepository brandJpaRepository;
  private final ProductJpaRepository productJpaRepository;
  private final ProductLikeJpaRepository productLikeJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  LikeApiE2ETest(
      TestRestTemplate restTemplate,
      BrandJpaRepository brandJpaRepository,
      ProductJpaRepository productJpaRepository,
      ProductLikeJpaRepository productLikeJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.restTemplate = restTemplate;
    this.brandJpaRepository = brandJpaRepository;
    this.productJpaRepository = productJpaRepository;
    this.productLikeJpaRepository = productLikeJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("POST /api/v1/like/products/{productId}")
  class LikeProduct {

    @Test
    @DisplayName("상품 좋아요 등록에 성공하면 200과 함께 좋아요 수가 증가한다")
    void likesProductSuccessfully() {
      Brand brand = brandJpaRepository.save(Brand.of("브랜드A", "설명"));
      Product product = productJpaRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(10L), brand.getId())
      );

      ResponseEntity<ApiResponse<Object>> response =
          restTemplate.exchange(
              BASE_URL + "/" + product.getId(),
              HttpMethod.POST,
              new HttpEntity<>(null, userHeaders(1L)),
              new ParameterizedTypeReference<>() {
              }
          );

      Product updated = productJpaRepository.findById(product.getId()).orElseThrow();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(productLikeJpaRepository.existsByUserIdAndProductId(1L, product.getId())).isTrue(),
          () -> assertThat(updated.getLikeCount()).isEqualTo(1L)
      );
    }

    @Test
    @DisplayName("같은 사용자가 두 번 좋아요 요청해도 1개만 반영된다")
    void ensuresIdempotencyOnDuplicateRequests() {
      Brand brand = brandJpaRepository.save(Brand.of("브랜드A", "설명"));
      Product product = productJpaRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(10L), brand.getId())
      );

      HttpEntity<Void> request = new HttpEntity<>(null, userHeaders(1L));
      ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {
      };

      restTemplate.exchange(BASE_URL + "/" + product.getId(), HttpMethod.POST, request, responseType);
      restTemplate.exchange(BASE_URL + "/" + product.getId(), HttpMethod.POST, request, responseType);

      Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
      assertThat(updated.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 404를 반환한다")
    void returnsNotFoundWhenProductMissing() {
      ResponseEntity<ApiResponse<Object>> response =
          restTemplate.exchange(
              BASE_URL + "/999999",
              HttpMethod.POST,
              new HttpEntity<>(null, userHeaders(1L)),
              new ParameterizedTypeReference<>() {
              }
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/like/products/{productId}")
  class UnlikeProduct {

    @Test
    @DisplayName("좋아요 취소에 성공하면 좋아요 수가 감소한다")
    void unlikesProductSuccessfully() {
      Brand brand = brandJpaRepository.save(Brand.of("브랜드A", "설명"));
      Product product = productJpaRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(10L), brand.getId())
      );

      HttpEntity<Void> request = new HttpEntity<>(null, userHeaders(1L));
      ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {
      };
      restTemplate.exchange(BASE_URL + "/" + product.getId(), HttpMethod.POST, request, responseType);

      ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
          BASE_URL + "/" + product.getId(),
          HttpMethod.DELETE,
          request,
          responseType
      );

      Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(productLikeJpaRepository.existsByUserIdAndProductId(1L, product.getId())).isFalse(),
          () -> assertThat(updated.getLikeCount()).isZero()
      );
    }

    @Test
    @DisplayName("이미 취소된 좋아요를 다시 취소해도 200 응답한다")
    void ensuresIdempotentUnlikes() {
      Brand brand = brandJpaRepository.save(Brand.of("브랜드A", "설명"));
      Product product = productJpaRepository.save(
          Product.of("상품1", Money.of(10000L), "설명", Stock.of(10L), brand.getId())
      );

      HttpEntity<Void> request = new HttpEntity<>(null, userHeaders(1L));
      ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {
      };

      restTemplate.exchange(BASE_URL + "/" + product.getId(), HttpMethod.POST, request, responseType);
      restTemplate.exchange(BASE_URL + "/" + product.getId(), HttpMethod.DELETE, request, responseType);
      ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
          BASE_URL + "/" + product.getId(),
          HttpMethod.DELETE,
          request,
          responseType
      );

      Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(productLikeJpaRepository.existsByUserIdAndProductId(1L, product.getId())).isFalse(),
          () -> assertThat(updated.getLikeCount()).isZero()
      );
    }
  }

  @Nested
  @DisplayName("GET /api/v1/like/products")
  class GetLikedProducts {

    @Test
    @DisplayName("기본 정렬(latest)로 좋아요 상품 목록을 반환한다")
    void returnsLikedProductsSortedByLatest() {
      Long userId = 99L;
      Brand brand = brandJpaRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Product first = saveProductWithLikes("에어포스", 12000L, brand.getId(), 3L);
      Product second = saveProductWithLikes("조던", 10000L, brand.getId(), 2L);
      Product third = saveProductWithLikes("덩크", 9000L, brand.getId(), 1L);

      productLikeJpaRepository.save(ProductLike.of(userId, first.getId(), LocalDateTime.of(2025, 1, 3, 0, 0)));
      productLikeJpaRepository.save(ProductLike.of(userId, second.getId(), LocalDateTime.of(2025, 1, 2, 0, 0)));
      productLikeJpaRepository.save(ProductLike.of(userId, third.getId(), LocalDateTime.of(2025, 1, 1, 0, 0)));

      ResponseEntity<ApiResponse<LikedProductListResponse>> response =
          restTemplate.exchange(
              BASE_URL,
              HttpMethod.GET,
              new HttpEntity<>(null, userHeaders(userId)),
              new ParameterizedTypeReference<>() {
              }
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<LikedProductResponse> products = response.getBody().data().products();
      assertThat(products)
          .extracting(LikedProductResponse::productName)
          .containsExactly("에어포스", "조던", "덩크");
    }

    @Test
    @DisplayName("sort=product_name이면 상품명을 기준으로 정렬된다")
    void returnsProductsSortedByName() {
      Long userId = 77L;
      Brand brand = brandJpaRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Product first = saveProductWithLikes("조던", 20000L, brand.getId(), 5L);
      Product second = saveProductWithLikes("덩크", 15000L, brand.getId(), 6L);
      Product third = saveProductWithLikes("에어맥스", 5000L, brand.getId(), 2L);

      productLikeJpaRepository.save(ProductLike.of(userId, first.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, second.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, third.getId(), LocalDateTime.now()));

      ResponseEntity<ApiResponse<LikedProductListResponse>> response =
          restTemplate.exchange(
              BASE_URL + "?sort=product_name",
              HttpMethod.GET,
              new HttpEntity<>(null, userHeaders(userId)),
              new ParameterizedTypeReference<>() {
              }
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<LikedProductResponse> products = response.getBody().data().products();
      assertThat(products)
          .extracting(LikedProductResponse::productName)
          .containsExactly("덩크", "에어맥스", "조던");
    }

    @Test
    @DisplayName("sort=price_desc이면 가격이 높은 순으로 정렬된다")
    void returnsProductsSortedByPriceDesc() {
      Long userId = 55L;
      Brand brand = brandJpaRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Product cheap = saveProductWithLikes("페가수스", 10000L, brand.getId(), 1L);
      Product mid = saveProductWithLikes("코르테즈", 50000L, brand.getId(), 1L);
      Product expensive = saveProductWithLikes("에어조던", 100000L, brand.getId(), 1L);

      productLikeJpaRepository.save(ProductLike.of(userId, cheap.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, mid.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, expensive.getId(), LocalDateTime.now()));

      ResponseEntity<ApiResponse<LikedProductListResponse>> response =
          restTemplate.exchange(
              BASE_URL + "?sort=price_desc",
              HttpMethod.GET,
              new HttpEntity<>(null, userHeaders(userId)),
              new ParameterizedTypeReference<>() {
              }
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<LikedProductResponse> products = response.getBody().data().products();
      assertThat(products)
          .extracting(LikedProductResponse::productName)
          .containsExactly("에어조던", "코르테즈", "페가수스");
    }

    @Test
    @DisplayName("sort=price_asc이면 가격이 낮은 순으로 정렬된다")
    void returnsProductsSortedByPriceAsc() {
      Long userId = 66L;
      Brand brand = brandJpaRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Product cheap = saveProductWithLikes("페가수스", 10000L, brand.getId(), 1L);
      Product mid = saveProductWithLikes("코르테즈", 50000L, brand.getId(), 1L);
      Product expensive = saveProductWithLikes("에어조던", 100000L, brand.getId(), 1L);

      productLikeJpaRepository.save(ProductLike.of(userId, cheap.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, mid.getId(), LocalDateTime.now()));
      productLikeJpaRepository.save(ProductLike.of(userId, expensive.getId(), LocalDateTime.now()));

      ResponseEntity<ApiResponse<LikedProductListResponse>> response =
          restTemplate.exchange(
              BASE_URL + "?sort=price_asc",
              HttpMethod.GET,
              new HttpEntity<>(null, userHeaders(userId)),
              new ParameterizedTypeReference<>() {
              }
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<LikedProductResponse> products = response.getBody().data().products();
      assertThat(products)
          .extracting(LikedProductResponse::productName)
          .containsExactly("페가수스", "코르테즈", "에어조던");
    }
  }

  private Product saveProductWithLikes(String name, long price, Long brandId, long likeCount) {
    Product product = Product.of(name, Money.of(price), name + " 설명", Stock.of(10L), brandId);
    if (likeCount > 0) {
      product.increaseLikeCount((int) likeCount);
    }
    return productJpaRepository.save(product);
  }

  private HttpHeaders userHeaders(Long userId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(ApiHeaders.USER_ID, String.valueOf(userId));
    return headers;
  }
}
