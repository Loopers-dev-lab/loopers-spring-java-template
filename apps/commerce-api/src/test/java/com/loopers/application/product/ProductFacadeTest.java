package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductFacade 통합 테스트")
@SpringBootTest
class ProductFacadeTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private com.loopers.domain.product.ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("getProducts 메서드")
    @Nested
    class GetProductsTest {

        @DisplayName("성공 케이스: sort가 'latest'이면 생성일시 내림차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProducts_withSortLatest_returnsProductsSortedByCreatedAtDesc() throws InterruptedException {
            // arrange
            ProductInfo product1 = createAndSaveProduct("상품1", BigDecimal.valueOf(10000L), 10L);
            Thread.sleep(10); // 생성 시간 차이를 위해
            ProductInfo product2 = createAndSaveProduct("상품2", BigDecimal.valueOf(20000L), 20L);
            Thread.sleep(10);
            ProductInfo product3 = createAndSaveProduct("상품3", BigDecimal.valueOf(30000L), 30L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("latest")
                    .build();

            // act
            Page<ProductInfo> result = productFacade.getProducts(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).name()).isEqualTo("상품3"), // 최신순
                    () -> assertThat(result.getContent().get(1).name()).isEqualTo("상품2"),
                    () -> assertThat(result.getContent().get(2).name()).isEqualTo("상품1"),
                    () -> assertThat(result.getTotalElements()).isEqualTo(3L)
            );
        }

        @DisplayName("성공 케이스: sort가 'price_asc'이면 가격 오름차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProducts_withSortPriceAsc_returnsProductsSortedByPriceAsc() {
            // arrange
            ProductInfo product1 = createAndSaveProduct("상품1", BigDecimal.valueOf(30000L), 10L);
            ProductInfo product2 = createAndSaveProduct("상품2", BigDecimal.valueOf(10000L), 20L);
            ProductInfo product3 = createAndSaveProduct("상품3", BigDecimal.valueOf(20000L), 30L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("price_asc")
                    .build();

            // act
            Page<ProductInfo> result = productFacade.getProducts(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).price()).isEqualByComparingTo(BigDecimal.valueOf(10000L)),
                    () -> assertThat(result.getContent().get(1).price()).isEqualByComparingTo(BigDecimal.valueOf(20000L)),
                    () -> assertThat(result.getContent().get(2).price()).isEqualByComparingTo(BigDecimal.valueOf(30000L))
            );
        }

        @DisplayName("성공 케이스: sort가 'likes_desc'이면 좋아요 수 내림차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProducts_withSortLikesDesc_returnsProductsSortedByLikesDesc() {
            // arrange
            ProductInfo product1 = createAndSaveProduct("상품1", BigDecimal.valueOf(10000L), 10L);
            ProductInfo product2 = createAndSaveProduct("상품2", BigDecimal.valueOf(20000L), 30L);
            ProductInfo product3 = createAndSaveProduct("상품3", BigDecimal.valueOf(30000L), 20L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("likes_desc")
                    .build();

            // act
            Page<ProductInfo> result = productFacade.getProducts(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).likeCount()).isEqualTo(30L),
                    () -> assertThat(result.getContent().get(1).likeCount()).isEqualTo(20L),
                    () -> assertThat(result.getContent().get(2).likeCount()).isEqualTo(10L)
            );
        }

        @DisplayName("성공 케이스: 페이징이 제대로 동작한다")
        @Test
        void getProducts_withPaging_returnsPagedResults() {
            // arrange
            for (int i = 1; i <= 5; i++) {
                createAndSaveProduct("상품" + i, BigDecimal.valueOf(10000L * i), 10L);
            }

            Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 페이지당 2개
            ProductCondition condition = ProductCondition.builder()
                    .sort("latest")
                    .build();

            // act
            Page<ProductInfo> result = productFacade.getProducts(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(2),
                    () -> assertThat(result.getNumber()).isEqualTo(1),
                    () -> assertThat(result.getSize()).isEqualTo(2),
                    () -> assertThat(result.getTotalElements()).isEqualTo(5L),
                    () -> assertThat(result.getTotalPages()).isEqualTo(3)
            );
        }
    }

    @DisplayName("getProduct 메서드")
    @Nested
    class GetProductTest {

        @DisplayName("성공 케이스: 존재하는 Product ID로 조회하면 해당 ProductInfo를 반환한다")
        @Test
        void getProduct_withValidId_returnsProductInfo() {
            // arrange
            ProductInfo productInfo = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 10L);
            Long productId = productInfo.id();

            // act
            ProductInfo result = productFacade.getProduct(productId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.id()).isEqualTo(productId),
                    () -> assertThat(result.name()).isEqualTo("테스트 상품"),
                    () -> assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(10000L)),
                    () -> assertThat(result.likeCount()).isEqualTo(10L),
                    () -> assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE),
                    () -> assertThat(result.isVisible()).isTrue(),
                    () -> assertThat(result.isSellable()).isTrue()
            );
        }

        @DisplayName("실패 케이스: 존재하지 않는 Product ID로 조회하면 NOT_FOUND 예외가 발생한다")
        @Test
        void getProduct_withInvalidId_throwsNotFoundException() {
            // arrange
            Long invalidId = 999L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProduct(invalidId);
            });

            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND),
                    () -> assertThat(exception.getCustomMessage()).contains("[productId = " + invalidId + "] Product를 찾을 수 없습니다.")
            );
        }
    }

    // 테스트 헬퍼 메서드
    private ProductInfo createAndSaveProduct(String name, BigDecimal price, Long likeCount) {
        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(price)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        // 먼저 Product 저장
        ProductInfo savedProductInfo = productFacade.saveProduct(ProductInfo.from(product));
        
        // 저장 후 likeCount를 reflection으로 설정
        Product savedProduct = productRepository.findById(savedProductInfo.id())
                .orElseThrow(() -> new RuntimeException("Product를 찾을 수 없습니다"));
        try {
            Field likeCountField = Product.class.getDeclaredField("likeCount");
            likeCountField.setAccessible(true);
            likeCountField.set(savedProduct, likeCount);
            productRepository.save(savedProduct); // 다시 저장하여 likeCount 반영
        } catch (Exception e) {
            throw new RuntimeException("likeCount 설정 실패", e);
        }
        
        // 최종 ProductInfo 반환
        return ProductInfo.from(productRepository.findById(savedProductInfo.id())
                .orElseThrow(() -> new RuntimeException("Product를 찾을 수 없습니다")));
    }
}

