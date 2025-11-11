package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
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
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiE2ETest {
    private static final String ENDPOINT = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    private final ProductJpaRepository productJpaRepository;
    private final BrandJpaRepository brandJpaRepository;

    @Autowired
    public ProductApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            ProductJpaRepository productJpaRepository,    
            BrandJpaRepository brandJpaRepository

    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.productJpaRepository = productJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
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
            Brand brandA = brandJpaRepository.save(
                    Brand.create("브랜드A")
            );

            Product productA = productJpaRepository.save(
                    Product.create("상품A", 10_000, brandA.getId())
            );

            String url = ENDPOINT + "?page=0&size=20";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {};

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
    }
}
