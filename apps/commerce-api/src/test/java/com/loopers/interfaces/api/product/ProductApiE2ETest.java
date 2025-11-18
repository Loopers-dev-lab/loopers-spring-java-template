package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.ProductLikeDomainService;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiE2ETest {
    private static final String ENDPOINT = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    private final ProductJpaRepository productJpaRepository;
    private final BrandJpaRepository brandJpaRepository;

    private final ProductLikeDomainService productLikeDomainService;

    @Autowired
    public ProductApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            ProductJpaRepository productJpaRepository,
            BrandJpaRepository brandJpaRepository,
            ProductLikeDomainService productLikeDomainService

    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.productJpaRepository = productJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.productLikeDomainService = productLikeDomainService;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("조건에 맞는 상품이 없을 경우, 빈 배열을 응답으로 반환한다.")
        @Test
        void productTest1() {
            // arrange
            String url = ENDPOINT + "?page=0&size=20";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).isEmpty(),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(0)
            );
        }


        @DisplayName("상품 1개가 있을 경우, 상품 목록을 응답으로 반환한다.")
        @Test
        void productTest2() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Product productA = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brandA.getId()));

            String url = ENDPOINT + "?page=0&size=20";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).hasSize(1),
                    () -> assertThat(response.getBody().data().products().get(0).name())
                            .isEqualTo("상품A"),
                    () -> assertThat(response.getBody().data().products().get(0).brand().name())
                            .isEqualTo("브랜드A")
            );
        }

        @DisplayName("특정 브랜드로 필터링할 경우, 해당 브랜드의 상품만 응답으로 반환한다.")
        @Test
        void productTest3() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Brand brandB = brandJpaRepository.save(Brand.create("브랜드B"));

            productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brandA.getId()));

            productJpaRepository.save(
                    Product.create("상품B", "설명", 20_000, 100L, brandB.getId()));

            String url = ENDPOINT + "?brandId=" + brandA.getId();

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).hasSize(1),
                    () -> assertThat(response.getBody().data().products().get(0).name()).isEqualTo("상품A"),
                    () -> assertThat(response.getBody().data().products().get(0).brand().name()).isEqualTo("브랜드A"));
        }

        @DisplayName("페이징을 적용할 경우, 지정한 페이지의 상품 목록을 응답으로 반환한다.")
        @Test
        void productTest4() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            // 30개 상품 생성
            for (int i = 1; i <= 30; i++) {
                productJpaRepository.save(Product.create("상품" + i, "설명", 10_000, 100L, brandA.getId()));
            }

            String url = ENDPOINT + "?page=1&size=10";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).hasSize(10),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(10),
                    () -> assertThat(response.getBody().data().products().get(0).name()).isEqualTo("상품20"),
                    () -> assertThat(response.getBody().data().products().get(9).name()).isEqualTo("상품11")
            );
        }

        @DisplayName("정렬 기준을 지정하지 않으면, 최신순으로 응답을 반환한다.")
        @Test
        void productTest5() throws InterruptedException {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Product product1 = productJpaRepository.save(
                    Product.create("상품1", "설명", 10_000, 100L, brandA.getId()));

            // 시간 차이
            Thread.sleep(100);

            Product product2 = productJpaRepository.save(
                    Product.create("상품2", "설명", 20_000, 100L, brandA.getId()));

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type
                    = new ParameterizedTypeReference<>() {
            };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response
                    = testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).hasSize(2),
                    () -> assertThat(response.getBody().data().products().get(0).name()).isEqualTo("상품2"),
                    () -> assertThat(response.getBody().data().products().get(1).name()).isEqualTo("상품1"));
        }

        @DisplayName("가격 오름차순으로 정렬할 경우, 낮은 가격 순으로 응답을 반환한다.")
        @Test
        void productTest6() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            productJpaRepository.save(
                    Product.create("상품 1", "설명", 200_000, 100L, brandA.getId()));

            productJpaRepository.save(
                    Product.create("상품 2", "설명", 100_000, 100L, brandA.getId()));

            productJpaRepository.save(
                    Product.create("상품 3", "설명", 150_000, 100L, brandA.getId()));

            String url = ENDPOINT + "?sort=price_asc";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody().data().products()).hasSize(3),
                    () -> assertThat(response.getBody().data().products().get(0).name()).isEqualTo("상품 2"),
                    () -> assertThat(response.getBody().data().products().get(0).price()).isEqualTo(100_000),
                    () -> assertThat(response.getBody().data().products().get(1).price()).isEqualTo(150_000),
                    () -> assertThat(response.getBody().data().products().get(2).price()).isEqualTo(200_000));
        }

        @DisplayName("존재하지 않는 브랜드로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void productTest7() {
            // arrange
            String url = ENDPOINT + "?brandId=999999";

            // act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getBody().meta().message()).contains("해당 브랜드를 찾을 수 없습니다"));
        }

        @DisplayName("비활성 브랜드로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void productTest8() {
            // arrange
            Brand inactiveBrand = brandJpaRepository.save(Brand.createInactive("비활성 브랜드"));

            String url = ENDPOINT + "?brandId=" + inactiveBrand.getId();

            // act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message()).contains("해당 브랜드를 찾을 수 없습니다"));
        }

        @DisplayName("유효하지 않은 정렬 키로 요청할 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void productTest9() {
            // arrange
            String url = ENDPOINT + "?sort=invalid_sort";

            // act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message()).contains("유효하지 않은 정렬 기준입니다"));
        }

        @DisplayName("좋아요 수 내림차순으로 정렬할 경우, 좋아요가 많은 순으로 응답을 반환한다.")
        @Test
        void productTest10() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Product product1 = productJpaRepository.save(
                    Product.create("상품 1", "설명", 200_000, 100L, brandA.getId()));
            product1.increaseLikes();
            productJpaRepository.save(product1);

            Product product2 = productJpaRepository.save(
                    Product.create("상품 2", "설명", 100_000, 100L, brandA.getId()));
            product2.increaseLikes();
            product2.increaseLikes();
            product2.increaseLikes();
            product2.increaseLikes();
            productJpaRepository.save(product2);

            Product product3 = productJpaRepository.save(
                    Product.create("상품 3", "설명", 150_000, 100L, brandA.getId()));
            product3.increaseLikes();
            productJpaRepository.save(product3);

            String url = ENDPOINT + "?sort=likes_desc";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody().data().products()).hasSize(3),
                    () -> assertThat(response.getBody().data().products().get(0).name()).isEqualTo("상품 2"),
                    () -> assertThat(response.getBody().data().products().get(1).name()).isEqualTo("상품 1"),
                    () -> assertThat(response.getBody().data().products().get(2).name()).isEqualTo("상품 3"));
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProduct {

        @DisplayName("상품 ID로 조회할 경우, 상품 상세 정보를 응답으로 반환한다.")
        @Test
        void productDetailTest1() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Product productA = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brandA.getId()));

            String url = ENDPOINT + "/" + productA.getId();

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductDetailResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductDetailResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(() -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(productA.getId()),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("상품A"),
                    () -> assertThat(response.getBody().data().totalLikes()).isEqualTo(0L),
                    () -> assertThat(response.getBody().data().brand().name()).isEqualTo("브랜드A"),
                    () -> assertThat(response.getBody().data().isLiked()).isNull());
        }

        @DisplayName("존재하지 않는 상품으로 조회할 경우, 404 에러를 반환한다.")
        @Test
        void productDetailTest2() {
            // arrange
            String url = ENDPOINT + "/999999";

            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message())
                            .contains("해당 상품을 찾을 수 없습니다")
            );
        }

        @DisplayName("삭제된 상품으로 조회할 경우, 404 에러를 반환한다.")
        @Test
        void productDetailTest3() {
            // arrange
            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));

            Product productA = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brandA.getId())
            );

            // 상품 삭제
            productA.delete();  // BaseEntity의 delete() 메서드
            productJpaRepository.save(productA);

            String url = ENDPOINT + "/" + productA.getId();

            // act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message())
                            .contains("해당 상품을 찾을 수 없습니다")
            );
        }
    }

}
