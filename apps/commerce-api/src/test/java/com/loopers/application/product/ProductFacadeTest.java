package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCondition;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.ProductView;
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

    @DisplayName("getProductViews 메서드")
    @Nested
    class GetProductViewsTest {

        @DisplayName("성공 케이스: sort가 'latest'이면 생성일시 내림차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProductViews_withSortLatest_returnsProductsSortedByCreatedAtDesc() throws InterruptedException {
            // arrange
            createAndSaveProduct("상품1", BigDecimal.valueOf(10000L), 10L);
            Thread.sleep(10); // 생성 시간 차이를 위해
            createAndSaveProduct("상품2", BigDecimal.valueOf(20000L), 20L);
            Thread.sleep(10);
            createAndSaveProduct("상품3", BigDecimal.valueOf(30000L), 30L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("latest")
                    .build();

            // act
            Page<ProductView> result = productFacade.getProductViews(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).getName()).isEqualTo("상품3"), // 최신순
                    () -> assertThat(result.getContent().get(1).getName()).isEqualTo("상품2"),
                    () -> assertThat(result.getContent().get(2).getName()).isEqualTo("상품1"),
                    () -> assertThat(result.getTotalElements()).isEqualTo(3L)
            );
        }

        @DisplayName("성공 케이스: sort가 'price_asc'이면 가격 오름차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProductViews_withSortPriceAsc_returnsProductsSortedByPriceAsc() {
            // arrange
            createAndSaveProduct("상품1", BigDecimal.valueOf(30000L), 10L);
            createAndSaveProduct("상품2", BigDecimal.valueOf(10000L), 20L);
            createAndSaveProduct("상품3", BigDecimal.valueOf(20000L), 30L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("price_asc")
                    .build();

            // act
            Page<ProductView> result = productFacade.getProductViews(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000L)),
                    () -> assertThat(result.getContent().get(1).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000L)),
                    () -> assertThat(result.getContent().get(2).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(30000L))
            );
        }

        @DisplayName("성공 케이스: sort가 'likes_desc'이면 좋아요 수 내림차순으로 정렬된 상품 목록을 반환한다")
        @Test
        void getProductViews_withSortLikesDesc_returnsProductsSortedByLikesDesc() {
            // arrange
            createAndSaveProduct("상품1", BigDecimal.valueOf(10000L), 10L);
            createAndSaveProduct("상품2", BigDecimal.valueOf(20000L), 30L);
            createAndSaveProduct("상품3", BigDecimal.valueOf(30000L), 20L);

            Pageable pageable = PageRequest.of(0, 10);
            ProductCondition condition = ProductCondition.builder()
                    .sort("likes_desc")
                    .build();

            // act
            Page<ProductView> result = productFacade.getProductViews(condition, pageable);

            // assert
            assertAll(
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(30L),
                    () -> assertThat(result.getContent().get(1).getLikeCount()).isEqualTo(20L),
                    () -> assertThat(result.getContent().get(2).getLikeCount()).isEqualTo(10L)
            );
        }

        @DisplayName("성공 케이스: 페이징이 제대로 동작한다")
        @Test
        void getProductViews_withPaging_returnsPagedResults() {
            // arrange
            for (int i = 1; i <= 5; i++) {
                createAndSaveProduct("상품" + i, BigDecimal.valueOf(10000L * i), 10L);
            }

            Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 페이지당 2개
            ProductCondition condition = ProductCondition.builder()
                    .sort("latest")
                    .build();

            // act
            Page<ProductView> result = productFacade.getProductViews(condition, pageable);

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

    @DisplayName("getProductView 메서드")
    @Nested
    class GetProductViewTest {

        @DisplayName("성공 케이스: 존재하는 Product ID로 조회하면 해당 ProductView를 반환한다")
        @Test
        void getProductView_withValidId_returnsProductView() {
            // arrange
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 10L);

            // act
            ProductView result = productFacade.getProductView(productId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getId()).isEqualTo(productId),
                    () -> assertThat(result.getName()).isEqualTo("테스트 상품"),
                    () -> assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000L)),
                    () -> assertThat(result.getLikeCount()).isEqualTo(10L),
                    () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.ON_SALE)
            );
        }

        @DisplayName("실패 케이스: 존재하지 않는 Product ID로 조회하면 NOT_FOUND 예외가 발생한다")
        @Test
        void getProductView_withInvalidId_throwsNotFoundException() {
            // arrange
            Long invalidId = 999L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProductView(invalidId);
            });

            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND),
                    () -> assertThat(exception.getCustomMessage()).contains("상품을 찾지 못했습니다.")
            );
        }
    }

    // 테스트 헬퍼 메서드
    private Long createAndSaveProduct(String name, BigDecimal price, Long likeCount) {
        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(price)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        // Product 저장
        Product savedProduct = productFacade.saveProduct(product);
        
        // 저장 후 likeCount를 reflection으로 설정
        try {
            Field likeCountField = Product.class.getDeclaredField("likeCount");
            likeCountField.setAccessible(true);
            likeCountField.set(savedProduct, likeCount);
            productRepository.save(savedProduct); // 다시 저장하여 likeCount 반영
        } catch (Exception e) {
            throw new RuntimeException("likeCount 설정 실패", e);
        }
        
        // productId 반환
        return savedProduct.getId();
    }
}

