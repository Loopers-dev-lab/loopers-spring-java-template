package com.loopers.interfaces.api.product;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ControllerE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired(required = false)
    private RedisTemplate<String, Object> productCacheTemplate;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화 (테스트 격리를 위해 각 테스트 시작 전 실행)
        if (productCacheTemplate != null) {
            try {
                var keys = productCacheTemplate.keys("product:detail:*");
                if (keys != null && !keys.isEmpty()) {
                    productCacheTemplate.delete(keys);
                }
            } catch (Exception e) {
                // Redis가 없는 환경에서는 무시
            }
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("상품 전체 목록 조회에 성공할 경우 상품 목록을 반환한다.")
        @Test
        void getProducts_success_returnProductList() {
            // given
            List<Brand> brands = new ArrayList<>();

            String[] brandNames = {"Brand 1", "Brand 2", "Brand 3"};

            for (String brandName : brandNames) {
                Brand brand = Brand.createBrand(brandName);
                brands.add(brand);
            }

            List<Brand> savedBrands = brandJpaRepository.saveAll(brands);

            int totalProducts = 20;
            Random random = new Random();

            List<Product> products = new ArrayList<>();

            for (int j = 0; j < totalProducts; j++) {
                String productCode = String.format("P%02d", j + 1);
                String productName = "ProductTest " + (j + 1);
                int stock = random.nextInt(1000); // 0 ~ 999
                BigDecimal price = BigDecimal.valueOf(random.nextInt(100000) + 100); // 100 ~ 100100
                Brand randomBrand = savedBrands.get(random.nextInt(savedBrands.size()));

                Product product = Product.createProduct(
                        productCode,
                        productName,
                        Money.of(price),
                        stock,
                        randomBrand
                );
                products.add(product);
            }

            productJpaRepository.saveAll(products);

            // when
            ResponseEntity<ApiResponse<ProductV1DTO.ProductsResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            assertThat(response.getBody().data()).isNotNull();

            ProductV1DTO.ProductsResponse productList = response.getBody().data();

            assertThat(productList.products()).hasSize(totalProducts);

        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProductDetail {

        @DisplayName("상품 상세 조회에 성공할 경우 상품 정보를 반환한다.")
        @Test
        void getProductDetail_success_returnProductInfo() {
            // given
            Brand brand = Brand.createBrand("나이키");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct(
                    "P001",
                    "에어맥스",
                    Money.of(150000),
                    50,
                    savedBrand
            );
            Product savedProduct = productJpaRepository.save(product);

            // when
            ResponseEntity<ApiResponse<ProductV1DTO.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products/" + savedProduct.getId(),
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            assertThat(response.getBody().data()).isNotNull();

            ProductV1DTO.ProductDetailResponse productDetail = response.getBody().data();

            assertAll(
                    // Product 정보 검증
                    () -> assertThat(productDetail.id()).isEqualTo(savedProduct.getId()),
                    () -> assertThat(productDetail.productCode()).isEqualTo("P001"),
                    () -> assertThat(productDetail.productName()).isEqualTo("에어맥스"),
                    () -> assertThat(productDetail.price()).isEqualByComparingTo(BigDecimal.valueOf(150000)),
                    () -> assertThat(productDetail.stock()).isEqualTo(50),
                    () -> assertThat(productDetail.likeCount()).isEqualTo(0L),
                    // Brand 정보 검증
                    () -> assertThat(productDetail.brand()).isNotNull(),
                    () -> assertThat(productDetail.brand().id()).isEqualTo(savedBrand.getId()),
                    () -> assertThat(productDetail.brand().brandName()).isEqualTo("나이키"),
                    () -> assertThat(productDetail.brand().isActive()).isTrue()
            );
        }

        @DisplayName("존재하지 않는 상품 조회 시 실패한다.")
        @Test
        void getProductDetail_withNonExistentProduct_fail() {
            // given
            Long nonExistentProductId = 99999L;

            // when
            ResponseEntity<ApiResponse<ProductV1DTO.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products/" + nonExistentProductId,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
            assertThat(response.getBody().meta().errorCode()).isEqualTo("Not Found");
        }
    }

}
