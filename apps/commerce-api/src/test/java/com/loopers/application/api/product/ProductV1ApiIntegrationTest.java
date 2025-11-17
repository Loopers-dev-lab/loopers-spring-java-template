package com.loopers.application.api.product;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import org.instancio.Instancio;
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

import static com.loopers.application.api.product.ProductV1Dto.GetProductListResponse;
import static com.loopers.application.api.product.ProductV1Dto.GetProductResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class ProductV1ApiIntegrationTest extends ApiIntegrationTest {

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
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                );
                brandId = brand.getId().value();

                Product product1 = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                );

                Product product2 = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                );
            }

            @Test
            @DisplayName("Status 200")
            void status200() {
                // When
                String endPoint = "/api/v1/products?brandId=" + brandId
                        + "&createdAtSort=&priceSort=&likeCountSort=ASC&pageNo=0&pageSize=10";

                ParameterizedTypeReference<ApiResponse<GetProductListResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<GetProductListResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }
    }

    @Nested
    @DisplayName("상품 정보 조회")
    class 상품_정보_조회 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            String productId;

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                );

                productId = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                ).getId().value();
            }

            @Test
            @DisplayName("Status 200")
            void status200() {
                // When
                String endPoint = "/api/v1/products/" + productId;

                ParameterizedTypeReference<ApiResponse<GetProductResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<GetProductResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @Test
            @DisplayName("Status 404")
            void status404() {
                String endPoint = "/api/v1/products/99999";
                ParameterizedTypeReference<ApiResponse<GetProductResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<GetProductResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
