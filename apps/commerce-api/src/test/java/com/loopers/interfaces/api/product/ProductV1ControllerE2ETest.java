package com.loopers.interfaces.api.product;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ControllerE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProductDetail {

        @DisplayName("상품 상세 조회에 성공할 경우 상품 정보를 반환한다.")
        @Test
        void getProductDetail_success_returnProductInfo() {
            // given
            Brand brand = Brand.createBrand("나이키");
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct(
                    "P001",
                    "에어맥스",
                    Money.of(150000),
                    50,
                    savedBrand
            );
            Product savedProduct = productRepository.registerProduct(product);

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
