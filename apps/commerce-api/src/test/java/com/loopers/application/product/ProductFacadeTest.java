package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.product.event.ProductViewEventHandler;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductFacade 통합 테스트")
@SpringBootTest
class ProductFacadeTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductViewRepository productViewRepository;

    @Autowired
    private ProductViewEventHandler productViewEventHandler;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
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

    @DisplayName("updateProduct 메서드")
    @Nested
    class UpdateProductTest {

        @DisplayName("성공 케이스: 상품 정보 수정 후 ProductView가 업데이트된다")
        @Test
        void updateProduct_withValidProduct_updatesProductView() {
            // arrange
            Long productId = createAndSaveProduct("원본 상품명", BigDecimal.valueOf(10000L), 10L);
            
            // Product 조회
            Product product = productService.findById(productId);

            // Product 업데이트 (새로운 필드 값으로 재빌드)
            Product updatedProduct = Product.builder()
                    .name("수정된 상품명")
                    .description("수정된 설명")
                    .price(BigDecimal.valueOf(20000L))
                    .status(ProductStatus.SOLD_OUT)
                    .isVisible(true)
                    .isSellable(false)
                    .brandId(product.getBrandId())
                    .build();
            
            // ID 설정 (리플렉션 사용)
            setIdUsingReflection(updatedProduct, productId);
            
            // act
            productFacade.updateProduct(updatedProduct);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            ProductEvents.Updated updateEvent = new ProductEvents.Updated(
                    updatedProduct.getId(),
                    updatedProduct.getBrandId(),
                    updatedProduct.getName(),
                    updatedProduct.getPrice(),
                    updatedProduct.getStatus()
            );
            productViewEventHandler.handleUpdated(updateEvent);

            // assert
            ProductView productView = productViewRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("ProductView not found"));
            
            assertAll(
                    () -> assertThat(productView.getName()).isEqualTo("수정된 상품명"),
                    () -> assertThat(productView.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000L)),
                    () -> assertThat(productView.getStatus()).isEqualTo(ProductStatus.SOLD_OUT),
                    () -> assertThat(productView.getLikeCount()).isEqualTo(10L) // 좋아요 수는 변경되지 않음
            );
        }

        @DisplayName("성공 케이스: 브랜드 변경 시 ProductView의 brandName이 업데이트된다")
        @Test
        void updateProduct_withBrandChange_updatesBrandName() {
            // arrange
            // 브랜드 생성
            Brand brand1 = Brand.builder()
                    .name("브랜드1")
                    .description("브랜드1 설명")
                    .status(BrandStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .build();
            Brand savedBrand1 = brandJpaRepository.save(brand1);

            Brand brand2 = Brand.builder()
                    .name("브랜드2")
                    .description("브랜드2 설명")
                    .status(BrandStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .build();
            Brand savedBrand2 = brandJpaRepository.save(brand2);

            // 상품 생성 (브랜드1로)
            Product product = Product.builder()
                    .name("테스트 상품")
                    .description("테스트 설명")
                    .price(BigDecimal.valueOf(10000L))
                    .status(ProductStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .brandId(savedBrand1.getId())
                    .build();
            Product savedProduct = productFacade.createProduct(product);
            Long productId = savedProduct.getId();
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            ProductEvents.Created event = new ProductEvents.Created(
                    savedProduct.getId(),
                    savedProduct.getBrandId(),
                    savedProduct.getName(),
                    savedProduct.getPrice(),
                    savedProduct.getStatus()
            );
            productViewEventHandler.handleCreated(event);
            
            // 초기 ProductView 확인
            ProductView initialView = productViewRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("ProductView not found"));
            assertThat(initialView.getBrandName()).isEqualTo("브랜드1");

            // Product 업데이트 (브랜드2로 변경)
            Product updatedProduct = Product.builder()
                    .name("테스트 상품")
                    .description("테스트 설명")
                    .price(BigDecimal.valueOf(10000L))
                    .status(ProductStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .brandId(savedBrand2.getId())
                    .build();
            
            // ID 설정 (리플렉션 사용)
            setIdUsingReflection(updatedProduct, productId);

            // act
            productFacade.updateProduct(updatedProduct);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            ProductEvents.Updated updateEvent = new ProductEvents.Updated(
                    updatedProduct.getId(),
                    updatedProduct.getBrandId(),
                    updatedProduct.getName(),
                    updatedProduct.getPrice(),
                    updatedProduct.getStatus()
            );
            productViewEventHandler.handleUpdated(updateEvent);

            // assert
            ProductView updatedView = productViewRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("ProductView not found"));
            
            assertAll(
                    () -> assertThat(updatedView.getBrandId()).isEqualTo(savedBrand2.getId()),
                    () -> assertThat(updatedView.getBrandName()).isEqualTo("브랜드2")
            );
        }
    }

    // 테스트 헬퍼 메서드
    private Long createAndSaveProduct(String name, BigDecimal price, Long likeCount) {
        // Brand 생성
        Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트 브랜드 설명")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);
        
        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(price)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();

        // Product 저장
        Product savedProduct = productFacade.createProduct(product);
        
        // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
        ProductEvents.Created event = new ProductEvents.Created(
                savedProduct.getId(),
                savedProduct.getBrandId(),
                savedProduct.getName(),
                savedProduct.getPrice(),
                savedProduct.getStatus()
        );
        productViewEventHandler.handleCreated(event);
        
        // ProductView의 likeCount 업데이트 (테스트용)
        // 실제로는 Like 엔티티를 생성해야 하지만, 테스트 편의를 위해 직접 업데이트
        productViewRepository.updateLikeCount(savedProduct.getId(), likeCount);
        
        // productId 반환
        return savedProduct.getId();
    }



    /**
     * 리플렉션을 사용하여 BaseEntity의 id 필드를 설정하는 헬퍼 메서드
     */
    private void setIdUsingReflection(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID using reflection", e);
        }
    }
}

