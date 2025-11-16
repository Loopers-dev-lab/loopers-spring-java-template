package com.loopers.application.api.product;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ProductV1ApiTest extends ApiIntegrationTest {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Nested
    @DisplayName("상품 목록 조회")
    class 상품_목록_조회 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            String brandId;

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("guerlain"), new BrandDescription("명품 향수 브랜드"))
                );
                brandId = brand.getId().value();

                Product product1 = productRepository.save(
                        Product.create(
                                new BrandId(brandId),
                                new ProductName("라 푸파](향수)"),
                                new ProductPrice(new BigDecimal("150.00"))
                        )
                );

                Product product2 = productRepository.save(
                        Product.create(
                                new BrandId(brandId),
                                new ProductName("라로스 에엑셀렌즈(향수)"),
                                new ProductPrice(new BigDecimal("200.00"))
                        )
                );
            }

            @Test
            @DisplayName("브랜드 ID로 상품 목록을 조회한다.")
            void 브랜드_ID로_상품_목록을_조회한다() {
                // When
                String endPoint = "/api/v1/products/?brandId=" + brandId
                        + "&createdAtSort=&priceSort=&likeCountSort=ASC&pageNo=0&pageSize=10";

                ParameterizedTypeReference<ApiResponse<ProductV1Dto.GetProductListResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<ProductV1Dto.GetProductListResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // Then
                assertSoftly(softly -> {
                    softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    softly.assertThat(response.getBody()).isNotNull();
                    softly.assertThat(response.getBody().data()).isNotNull();
                    softly.assertThat(response.getBody().data().items()).hasSize(2);
                    softly.assertThat(response.getBody().data().totalElements()).isEqualTo(2);
                    softly.assertThat(response.getBody().data().totalPages()).isGreaterThan(0);
                });
            }
        }
    }
}
