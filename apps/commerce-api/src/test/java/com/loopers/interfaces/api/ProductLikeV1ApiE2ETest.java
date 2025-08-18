package com.loopers.interfaces.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.CommerceApiContextTest;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeRepository;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.like.ProductLikeV1Dto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("상품 좋아요 API E2E 테스트")
class ProductLikeV1ApiE2ETest extends CommerceApiContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    private final Long userId = 1L;
    private BrandModel testBrand;
    private ProductModel testProduct;

    @AfterEach
    void tearDown() {
        productLikeRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
    }

    private void setupTestData() {
        testBrand = brandRepository.save(BrandFixture.createBrand("테스트브랜드"));
        testProduct = productRepository.save(
                ProductFixture.createProductWithBrandId(null, testBrand.getId())
        );
    }

    @Nested
    @DisplayName("상품 좋아요 추가 API")
    class AddProductLikeApiTest {

        @Test
        @DisplayName("상품 좋아요 추가에 성공한다")
        void addProductLike_success() throws Exception {
            // Arrange
            setupTestData();

            // Act
            ResultActions result = mockMvc.perform(
                    post("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                    .andExpect(jsonPath("$.data.isLiked").value(true))
                    .andExpect(jsonPath("$.data.message").value("상품 좋아요를 추가했습니다."));

            // 실제 DB에 저장되었는지 확인
            boolean exists = productLikeRepository.existsByUserIdAndProductId(userId, testProduct.getId());
            assertThat(exists).isTrue();

            // 상품의 좋아요 수가 증가했는지 확인
            ProductModel updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(updatedProduct.getLikeCount().getValue()).isEqualTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 대해서는 멱등성을 보장한다")
        void addProductLike_idempotent() throws Exception {
            // Arrange
            setupTestData();
            ProductLikeModel existingLike = productLikeRepository.save(
                    ProductLikeModel.create(userId, testProduct.getId())
            );

            // Act
            ResultActions result = mockMvc.perform(
                    post("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isLiked").value(true));

            // 좋아요가 중복 생성되지 않았는지 확인
            long likeCount = productLikeRepository.count();
            assertThat(likeCount).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 상품에 좋아요 시 404 에러가 발생한다")
        void addProductLike_productNotFound() throws Exception {
            // Act & Assert
            mockMvc.perform(
                    post("/api/v1/like/products/{productId}", 99999L)
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("상품 좋아요 취소 API")
    class RemoveProductLikeApiTest {

        @Test
        @DisplayName("상품 좋아요 취소에 성공한다")
        void removeProductLike_success() throws Exception {
            // Arrange
            setupTestData();
            productLikeRepository.save(ProductLikeModel.create(userId, testProduct.getId()));

            // Act
            ResultActions result = mockMvc.perform(
                    delete("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                    .andExpect(jsonPath("$.data.isLiked").value(false))
                    .andExpect(jsonPath("$.data.message").value("상품 좋아요를 취소했습니다."));

            // 실제 DB에서 삭제되었는지 확인
            boolean exists = productLikeRepository.existsByUserIdAndProductId(userId, testProduct.getId());
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("좋아요하지 않은 상품 취소 시에도 멱등성을 보장한다")
        void removeProductLike_idempotent() throws Exception {
            // Arrange
            setupTestData();

            // Act
            ResultActions result = mockMvc.perform(
                    delete("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isLiked").value(false));
        }
    }

    @Nested
    @DisplayName("좋아요한 상품 목록 조회 API")
    class GetLikedProductsApiTest {

        @Test
        @DisplayName("좋아요한 상품 목록 조회에 성공한다")
        void getLikedProducts_success() throws Exception {
            // Arrange
            setupTestData();
            
            // 추가 테스트 데이터 생성
            ProductModel product2 = productRepository.save(
                    ProductFixture.createProductWithBrandId(null, testBrand.getId())
            );
            
            productLikeRepository.save(ProductLikeModel.create(userId, testProduct.getId()));
            productLikeRepository.save(ProductLikeModel.create(userId, product2.getId()));

            // Act
            ResultActions result = mockMvc.perform(
                    get("/api/v1/like/products")
                            .header("X-User-Id", userId)
                            .param("page", "0")
                            .param("size", "10")
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalCount").value(2))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items.length()").value(2))
                    .andExpect(jsonPath("$.data.items[0].brandName").value("테스트브랜드"))
                    .andExpect(jsonPath("$.data.items[0].likedAt").exists());
        }

        @Test
        @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
        void getLikedProducts_emptyList() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(
                    get("/api/v1/like/products")
                            .header("X-User-Id", userId)
                            .param("page", "0")
                            .param("size", "10")
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalCount").value(0))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        void getLikedProducts_pagination() throws Exception {
            // Arrange
            setupTestData();
            
            // 5개의 상품 좋아요 생성
            for (int i = 0; i < 5; i++) {
                ProductModel product = productRepository.save(
                        ProductFixture.createProductWithBrandId(null, testBrand.getId())
                );
                productLikeRepository.save(ProductLikeModel.create(userId, product.getId()));
            }

            // Act - 첫 번째 페이지 (size=2)
            ResultActions result = mockMvc.perform(
                    get("/api/v1/like/products")
                            .header("X-User-Id", userId)
                            .param("page", "0")
                            .param("size", "2")
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCount").value(6)) // testProduct + 5개
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(2))
                    .andExpect(jsonPath("$.data.items.length()").value(2));
        }
    }

    @Nested
    @DisplayName("전체 플로우 통합 테스트")
    class IntegrationFlowTest {

        @Test
        @DisplayName("좋아요 추가 → 목록 조회 → 취소 → 목록 조회 플로우가 정상 동작한다")
        void fullLikeFlow_worksCorrectly() throws Exception {
            // Arrange
            setupTestData();

            // Act & Assert
            // 1. 좋아요 추가
            mockMvc.perform(
                    post("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
            ).andExpect(status().isOk())
             .andExpect(jsonPath("$.data.isLiked").value(true));

            // 2. 목록 조회 - 1개 있어야 함
            mockMvc.perform(
                    get("/api/v1/like/products")
                            .header("X-User-Id", userId)
            ).andExpect(status().isOk())
             .andExpect(jsonPath("$.data.totalCount").value(1));

            // 3. 좋아요 취소
            mockMvc.perform(
                    delete("/api/v1/like/products/{productId}", testProduct.getId())
                            .header("X-User-Id", userId)
            ).andExpect(status().isOk())
             .andExpect(jsonPath("$.data.isLiked").value(false));

            // 4. 목록 조회 - 0개여야 함
            mockMvc.perform(
                    get("/api/v1/like/products")
                            .header("X-User-Id", userId)
            ).andExpect(status().isOk())
             .andExpect(jsonPath("$.data.totalCount").value(0));
        }
    }
}