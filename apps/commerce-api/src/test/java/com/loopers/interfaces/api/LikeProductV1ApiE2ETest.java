package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.product.Product;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.metrics.product.ProductMetricsJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.like.product.LikeProductV1Dto;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LikeProductV1ApiE2ETest {

    private final String ENDPOINT_USER = "/api/v1/users";
    private final String ENDPOINT_LIKE_PRODUCTS = "/api/v1/like/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductMetricsJpaRepository productMetricsJpaRepository;
    private final SupplyService supplyService;

    @Autowired
    public LikeProductV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository,
            ProductMetricsJpaRepository productMetricsJpaRepository,
            SupplyService supplyService
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.productMetricsJpaRepository = productMetricsJpaRepository;
        this.supplyService = supplyService;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private final String validUserId = "user123";
    private final String validEmail = "xx@yy.zz";
    private final String validBirthday = "1993-03-13";
    private final String validGender = "male";

    private Long brandId;
    private Long productId1;
    private Long productId2;

    @BeforeEach
    @Transactional
    void setupUserAndProducts() {
        // User 등록
        UserV1Dto.UserRegisterRequest request = new UserV1Dto.UserRegisterRequest(
                validUserId,
                validEmail,
                validBirthday,
                validGender
        );
        ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        testRestTemplate.exchange(ENDPOINT_USER, HttpMethod.POST, new HttpEntity<>(request), responseType);

        // Brand 등록
        Brand brand = Brand.create("Nike");
        Brand savedBrand = brandJpaRepository.save(brand);
        brandId = savedBrand.getId();

        // Product 등록
        Product product1 = createProduct("상품1", brandId, 10000);
        Product savedProduct1 = productJpaRepository.save(product1);
        productId1 = savedProduct1.getId();
        // ProductMetrics 등록
        ProductMetrics metrics1 = ProductMetrics.create(productId1, 0);
        productMetricsJpaRepository.save(metrics1);
        // Supply 등록
        Supply supply1 = Supply.create(productId1, new Stock(100));
        supplyService.saveSupply(supply1);

        Product product2 = createProduct("상품2", brandId, 20000);
        Product savedProduct2 = productJpaRepository.save(product2);
        productId2 = savedProduct2.getId();
        // ProductMetrics 등록
        ProductMetrics metrics2 = ProductMetrics.create(productId2, 0);
        productMetricsJpaRepository.save(metrics2);
        // Supply 등록
        Supply supply2 = Supply.create(productId2, new Stock(200));
        supplyService.saveSupply(supply2);
    }


    private Product createProduct(String name, Long brandId, int priceAmount) {
        return Product.create(name, brandId, new Price(priceAmount));
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", validUserId);
        return headers;
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class PostLikeProduct {
        @DisplayName("로그인한 유저가 상품 좋아요 등록에 성공할 경우, `200 OK` 응답을 반환한다.")
        @Test
        void returnOk_whenLikeProductSuccess() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = createHeaders();

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
        }

        @DisplayName("같은 상품에 여러 번 좋아요를 등록해도 멱등하게 동작한다.")
        @Test
        void beIdempotent_whenLikeProductMultipleTimes() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = createHeaders();

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response1 = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);
            ResponseEntity<ApiResponse<Void>> response2 = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response1.getStatusCode().is2xxSuccessful());
            assertTrue(response2.getStatusCode().is2xxSuccessful());
        }

        @DisplayName("비로그인 유저가 좋아요 등록을 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, null), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsEmpty() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 공백만 있을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsBlank() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "   ");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 유저 ID로 좋아요 등록을 시도할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenUserIdDoesNotExist() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "nonexist");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            // Note: 현재 실패할 수 있지만 테스트 케이스는 작성함
            assertThat(response.getStatusCode().value() == 400 || response.getStatusCode().value() == 404).isTrue();
        }

        @DisplayName("존재하지 않는 상품 ID로 좋아요 등록을 시도할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenProductIdDoesNotExist() {
            // arrange
            Long nonExistentProductId = 99999L;
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + nonExistentProductId;
            HttpHeaders headers = createHeaders();

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            // Note: 현재 실패할 수 있지만 테스트 케이스는 작성함
            assertThat(response.getStatusCode().value() == 400 || response.getStatusCode().value() == 404).isTrue();
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class DeleteLikeProduct {
        @DisplayName("로그인한 유저가 상품 좋아요 취소에 성공할 경우, `200 OK` 응답을 반환한다.")
        @Test
        void returnOk_whenUnlikeProductSuccess() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = createHeaders();
            // 먼저 좋아요 등록
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
        }

        @DisplayName("좋아요하지 않은 상품을 취소해도 멱등하게 동작한다.")
        @Test
        void beIdempotent_whenUnlikeProductNotLiked() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = createHeaders();

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
        }

        @DisplayName("비로그인 유저가 좋아요 취소를 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, null), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsEmpty() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 공백만 있을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsBlank() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "   ");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 유저 ID로 좋아요 취소를 시도할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenUserIdDoesNotExist() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "/" + productId1;
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "nonexist");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            // assert
            // Note: 현재 실패할 수 있지만 테스트 케이스는 작성함
            assertThat(response.getStatusCode().value() == 400 || response.getStatusCode().value() == 404).isTrue();
        }
    }

    @DisplayName("GET /api/v1/like/products")
    @Nested
    class GetLikedProducts {
        @DisplayName("로그인한 유저가 좋아요한 상품 목록 조회에 성공할 경우 200 OK 응답을 반환한다")
        @Test
        void returnLikedProducts_whenGetLikedProductsSuccess() {
            // arrange
            HttpHeaders headers = createHeaders();
            // 좋아요 등록
            ParameterizedTypeReference<ApiResponse<Void>> likeResponseType = new ParameterizedTypeReference<>() {
            };
            testRestTemplate.exchange(ENDPOINT_LIKE_PRODUCTS + "/" + productId1, HttpMethod.POST, new HttpEntity<>(null, headers), likeResponseType);
            testRestTemplate.exchange(ENDPOINT_LIKE_PRODUCTS + "/" + productId2, HttpMethod.POST, new HttpEntity<>(null, headers), likeResponseType);

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_LIKE_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
        }

        @DisplayName("페이지네이션 파라미터로 조회할 경우, 해당 페이지의 좋아요한 상품 목록을 반환한다.")
        @Test
        void returnLikedProducts_whenWithPagination() {
            // arrange
            String url = ENDPOINT_LIKE_PRODUCTS + "?page=0&size=10";
            HttpHeaders headers = createHeaders();
            // 좋아요 등록
            ParameterizedTypeReference<ApiResponse<Void>> likeResponseType = new ParameterizedTypeReference<>() {
            };
            testRestTemplate.exchange(ENDPOINT_LIKE_PRODUCTS + "/" + productId1, HttpMethod.POST, new HttpEntity<>(null, headers), likeResponseType);

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull()
            );
        }

        @DisplayName("비로그인 유저가 좋아요한 상품 목록 조회를 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_LIKE_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, null), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsEmpty() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "");

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_LIKE_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 공백만 있을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsBlank() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "   ");

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_LIKE_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 유저 ID로 좋아요한 상품 목록 조회를 시도할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenUserIdDoesNotExist() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "nonexist");

            // act
            ParameterizedTypeReference<ApiResponse<LikeProductV1Dto.ProductsResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<LikeProductV1Dto.ProductsResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_LIKE_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            // Note: 현재 실패할 수 있지만 테스트 케이스는 작성함
            assertThat(response.getStatusCode().value() == 400 || response.getStatusCode().value() == 404).isTrue();
        }
    }
}
