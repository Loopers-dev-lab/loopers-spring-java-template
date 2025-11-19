package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.productlike.ProductLike;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.productlike.ProductLikeJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductListItemResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductListResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductResponse;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
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
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiE2ETest {

  private static final String BASE_URL = "/api/v1/products";
  private static final LocalDate FIXED_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDateTime FIXED_DATE_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);
  private static final ParameterizedTypeReference<ApiResponse<ProductListResponse>> PRODUCT_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<ApiResponse<ProductResponse>> PRODUCT_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final TestRestTemplate testRestTemplate;
  private final ProductJpaRepository productJpaRepository;
  private final BrandJpaRepository brandJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final ProductLikeJpaRepository productLikeJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  ProductApiE2ETest(
      TestRestTemplate testRestTemplate,
      ProductJpaRepository productJpaRepository,
      BrandJpaRepository brandJpaRepository,
      UserJpaRepository userJpaRepository,
      ProductLikeJpaRepository productLikeJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.productJpaRepository = productJpaRepository;
    this.brandJpaRepository = brandJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.productLikeJpaRepository = productLikeJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("GET /api/v1/products")
  class GetProducts {

    @Test
    @DisplayName("기본 정렬(latest)로 상품 목록과 페이징 정보를 반환한다")
    void returnsProductsSortedByLatest_whenSortIsDefault() {
      Brand brand = saveBrand("나이키");
      Product first = saveProduct("슬리퍼", 12000L, 10L, brand.getId(), 0);
      Product second = saveProduct("샌들", 15000L, 5L, brand.getId(), 0);

      ResponseEntity<ApiResponse<ProductListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("page", 0)
                  .queryParam("size", 20)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              HttpEntity.EMPTY,
              PRODUCT_LIST_RESPONSE_TYPE
          );

      ApiResponse<ProductListResponse> responseBody = response.getBody();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(responseBody).isNotNull(),
          () -> {
            ProductListResponse data = responseBody.data();
            assertThat(data).isNotNull();
            List<ProductListItemResponse> products = data.products();
            assertThat(products)
                .hasSize(2)
                .element(0).extracting(ProductListItemResponse::productId)
                .isEqualTo(second.getId());
            assertThat(products)
                .element(1).extracting(ProductListItemResponse::productId)
                .isEqualTo(first.getId());
          }
      );
    }

    @Test
    @DisplayName("브랜드 필터 + sort=price_asc 조합으로 좋아요 상태를 포함해 반환한다")
    void returnsFilteredProductsWithLikeStatus_whenBrandAndPriceSortProvided() {
      Brand nike = saveBrand("나이키");
      Brand adidas = saveBrand("아디다스");
      Product nikeProduct = saveProduct("에어맥스", 200000L, 50L, nike.getId(), 5);
      saveProduct("울트라부스트", 150000L, 20L, adidas.getId(), 0);
      User user = saveUser("tester1");
      productLikeJpaRepository.save(ProductLike.of(user.getId(), nikeProduct.getId(), FIXED_DATE_TIME));

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", String.valueOf(user.getId()));

      ResponseEntity<ApiResponse<ProductListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("brandId", nike.getId())
                  .queryParam("sort", "price_asc")
                  .queryParam("page", 0)
                  .queryParam("size", 10)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              new HttpEntity<>(null, headers),
              PRODUCT_LIST_RESPONSE_TYPE
          );

      ApiResponse<ProductListResponse> responseBody = response.getBody();

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(responseBody).isNotNull(),
          () -> {
            ProductListResponse data = responseBody.data();
            assertThat(data).isNotNull();
            List<ProductListItemResponse> products = data.products();
            assertThat(products)
                .hasSize(1)
                .element(0)
                .extracting("productId", "likeCount", "isLiked")
                .containsExactly(nikeProduct.getId(), 5L, true);
          }
      );
    }

    @Test
    @DisplayName("sort=price_asc이면 가격 오름차순으로 정렬한다")
    void returnsProductsSortedByPriceAscending_whenSortIsPriceAsc() {
      Brand brand = saveBrand("가격브랜드");
      Product cheap = saveProduct("슬림핏", 10000L, 10L, brand.getId(), 0);
      Product pricey = saveProduct("프리미엄", 50000L, 8L, brand.getId(), 0);

      ResponseEntity<ApiResponse<ProductListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("sort", "price_asc")
                  .queryParam("size", 10)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              HttpEntity.EMPTY,
              PRODUCT_LIST_RESPONSE_TYPE
          );

      ApiResponse<ProductListResponse> responseBody = response.getBody();
      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(responseBody).isNotNull(),
          () -> {
            ProductListResponse data = responseBody.data();
            assertThat(data).isNotNull();
            List<ProductListItemResponse> products = data.products();
            assertThat(products).extracting(ProductListItemResponse::productId)
                .containsExactly(cheap.getId(), pricey.getId());
          }
      );
    }

    @Test
    @DisplayName("sort=likes_desc이면 좋아요 내림차순으로 정렬한다")
    void returnsProductsSortedByLikesDescending_whenSortIsLikesDesc() {
      Brand brand = saveBrand("좋아요브랜드");
      Product popular = saveProduct("인기상품", 20000L, 10L, brand.getId(), 10);
      Product normal = saveProduct("일반상품", 18000L, 10L, brand.getId(), 2);

      ResponseEntity<ApiResponse<ProductListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("sort", "likes_desc")
                  .queryParam("size", 10)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              HttpEntity.EMPTY,
              PRODUCT_LIST_RESPONSE_TYPE
          );

      ApiResponse<ProductListResponse> responseBody = response.getBody();
      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(responseBody).isNotNull(),
          () -> {
            ProductListResponse data = responseBody.data();
            assertThat(data).isNotNull();
            List<ProductListItemResponse> products = data.products();
            assertThat(products).extracting(ProductListItemResponse::productId)
                .containsExactly(popular.getId(), normal.getId());
          }
      );
    }
  }

  @Nested
  @DisplayName("GET /api/v1/products/{productId}")
  class GetProductDetail {

    @Test
    @DisplayName("존재하는 상품이면 상세 정보를 반환한다")
    void returnsProductDetail_whenProductExists() {
      Brand brand = saveBrand("루프");
      Product product = saveProduct("루프 티셔츠", 35000L, 30L, brand.getId(), 2);
      User user = saveUser("tester2");
      productLikeJpaRepository.save(ProductLike.of(user.getId(), product.getId(), FIXED_DATE_TIME));

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", String.valueOf(user.getId()));

      ResponseEntity<ApiResponse<ProductResponse>> response =
          testRestTemplate.exchange(
              BASE_URL + "/" + product.getId(),
              HttpMethod.GET,
              new HttpEntity<>(null, headers),
              PRODUCT_RESPONSE_TYPE
          );

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> {
            ApiResponse<ProductResponse> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            ProductResponse productResponse = responseBody.data();
            assertThat(productResponse).isNotNull();
            assertThat(productResponse)
                .extracting(
                    ProductResponse::productId,
                    ProductResponse::name,
                    productBrand -> productBrand.brand().brandId(),
                    ProductResponse::isLiked,
                    ProductResponse::likeCount
                )
                .containsExactly(product.getId(), "루프 티셔츠", brand.getId(), true, 2L);
          }
      );
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 404를 반환한다")
    void returnsNotFound_whenProductDoesNotExist() {
      ResponseEntity<ApiResponse<ProductResponse>> response =
          testRestTemplate.exchange(
              BASE_URL + "/999999",
              HttpMethod.GET,
              HttpEntity.EMPTY,
              PRODUCT_RESPONSE_TYPE
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  private Brand saveBrand(String name) {
    return brandJpaRepository.save(Brand.of(name, name + " 설명"));
  }

  private Product saveProduct(String name, long price, long stock, Long brandId, long likeCount) {
    Product product = Product.of(name, Money.of(price), name + " 상세", Stock.of(stock), brandId);
    if (likeCount > 0) {
      product.increaseLikeCount((int) likeCount);
    }
    return productJpaRepository.save(product);
  }

  private User saveUser(String loginId) {
    return userJpaRepository.save(User.of(loginId, loginId + "@loopers.dev", FIXED_DATE.minusYears(20), Gender.MALE, FIXED_DATE));
  }
}
