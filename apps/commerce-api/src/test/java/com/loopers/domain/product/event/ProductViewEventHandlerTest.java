package com.loopers.domain.product.event;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.cache.ProductCacheService;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductViewEventHandler 테스트")
@SpringBootTest
class ProductViewEventHandlerTest {

    @Autowired
    private ProductViewEventHandler eventHandler;
    
    @Autowired
    private ProductCacheService productCacheService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductViewRepository productViewRepository;
    
    @Autowired
    private RedisTemplate<String, Object> productCacheRedisTemplate;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    
    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("Write-Through: 좋아요 수 변경 테스트")
    @Nested
    class WriteThroughTest {

        @DisplayName("캐시가 존재할 때 좋아요 증가하면 캐시 값이 증가한다")
        @Test
        void likeIncrement_withExistingCache_incrementsValue() throws InterruptedException {
            // arrange
            // 상품 생성 및 ProductView 생성 대기
            Product product = createAndSaveProduct("테스트 상품");
            Long productId = product.getId();
            waitForProductViewCreation(productId);
            
            // 캐시 웜업 (Stat 캐시 생성)
            productCacheService.getProductView(productId);
            waitForAsyncProcessing();
            
            // 초기 Stat 캐시 확인
            Object initialCount = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(initialCount).isNotNull();
            long oldCount = initialCount != null ? Long.parseLong(initialCount.toString()) : 0L;
            
            // act - 좋아요 증가 이벤트 발행
            eventHandler.handleProductLikeCountEvent(ProductEventDto.LikeCount.increment(productId));
            waitForAsyncProcessing();
            
            // assert
            Object cachedCount = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedCount).isNotNull();
            long newCount = cachedCount != null ? Long.parseLong(cachedCount.toString()) : 0L;
            assertThat(newCount).isEqualTo(oldCount + 1);
        }

        @DisplayName("캐시가 존재할 때 좋아요 감소하면 캐시 값이 감소한다")
        @Test
        void likeDecrement_withExistingCache_decrementsValue() throws InterruptedException {
            // arrange
            // 상품 생성 및 ProductView 생성 대기
            Product product = createAndSaveProduct("테스트 상품");
            Long productId = product.getId();
            waitForProductViewCreation(productId);
            
            // 캐시 웜업 (Stat 캐시 생성)
            productCacheService.getProductView(productId);
            waitForAsyncProcessing();
            
            // 초기값을 10으로 설정
            productCacheRedisTemplate.opsForHash().put("product:stat:" + productId, "likeCount", "10");
            
            // act - 좋아요 감소 이벤트 발행
            eventHandler.handleProductLikeCountEvent(ProductEventDto.LikeCount.decrement(productId));
            waitForAsyncProcessing();
            
            // assert
            Object cachedCount = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedCount).isEqualTo("9");
        }

        @DisplayName("좋아요 감소 시 음수가 되지 않는다")
        @Test
        void likeDecrement_doesNotGoBelowZero() throws InterruptedException {
            // arrange
            // 상품 생성 및 ProductView 생성 대기
            Product product = createAndSaveProduct("테스트 상품");
            Long productId = product.getId();
            waitForProductViewCreation(productId);
            
            // 캐시 웜업 (Stat 캐시 생성)
            productCacheService.getProductView(productId);
            waitForAsyncProcessing();
            
            // 초기값을 0으로 설정
            productCacheRedisTemplate.opsForHash().put("product:stat:" + productId, "likeCount", "0");
            
            // act - 좋아요 감소 이벤트 발행
            eventHandler.handleProductLikeCountEvent(ProductEventDto.LikeCount.decrement(productId));
            waitForAsyncProcessing();
            
            // assert
            Object cachedCount = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedCount).isEqualTo("0");
        }

        @DisplayName("캐시가 없으면 캐시 업데이트를 건너뛴다 (Write-Around)")
        @Test
        void likeChange_withoutCache_skipsCache() throws InterruptedException {
            // arrange - 캐시 없음 (상품만 생성)
            Product product = createAndSaveProduct("테스트 상품");
            Long productId = product.getId();
            
            // act - 좋아요 증가 이벤트 발행
            eventHandler.handleProductLikeCountEvent(ProductEventDto.LikeCount.increment(productId));
            waitForAsyncProcessing();
            
            // assert - 캐시가 생성되지 않음
            Object cachedCount = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedCount).isNull();
        }
    }

    @DisplayName("Write-Around: 상품 생성 테스트")
    @Nested
    class WriteAroundTest {

        @DisplayName("상품 생성 시 캐시에 쓰지 않는다")
        @Test
        void productCreated_doesNotWriteToCache() throws InterruptedException {
            // arrange
            Product product = Product.builder()
                    .name("새 상품")
                    .description("설명")
                    .price(BigDecimal.valueOf(10000))
                    .status(ProductStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .build();

            // act - 상품 생성 (이벤트 발행)
            productService.createProduct(product);
            waitForAsyncProcessing();
            
            Long productId = product.getId();
            
            // assert - ProductView는 DB에 생성되었지만 캐시에는 없음
            Optional<ProductView> productView = productViewRepository.findById(productId);
            assertThat(productView).isPresent();
            
            Object cachedInfo = productCacheRedisTemplate.opsForValue().get("product:info:" + productId);
            assertThat(cachedInfo).isNull(); // Write-Around: 캐시에 쓰지 않음
        }
    }

    @DisplayName("Evict: 캐시 삭제 테스트")
    @Nested
    class EvictTest {

        @DisplayName("상품 수정 시 Info 캐시가 삭제된다")
        @Test
        void productUpdate_evictsInfoCache() throws InterruptedException {
            // arrange
            Product product = createAndSaveProduct("원본 상품");
            Long productId = product.getId();
            waitForProductViewCreation(productId);
            
            // 캐시 웜업
            productCacheService.getProductView(productId);
            waitForAsyncProcessing();
            
            // 캐시 존재 확인
            Object cachedInfo = productCacheRedisTemplate.opsForValue().get("product:info:" + productId);
            assertThat(cachedInfo).isNotNull();
            
            // act - 상품 수정 (이벤트 발행)
            Product updatedProduct = Product.builder()
                    .name("수정된 상품")
                    .description("수정된 설명")
                    .price(BigDecimal.valueOf(20000))
                    .status(ProductStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .brandId(product.getBrandId())
                    .build();
            
            // ID 설정 (리플렉션 사용)
            setIdUsingReflection(updatedProduct, productId);
            productService.updateProduct(updatedProduct);
            waitForAsyncProcessing();
            
            // assert - Info 캐시가 삭제됨
            Object cachedInfoAfterUpdate = productCacheRedisTemplate.opsForValue().get("product:info:" + productId);
            assertThat(cachedInfoAfterUpdate).isNull();
        }

        @DisplayName("상품 삭제 시 Info와 Stat 캐시가 모두 삭제된다")
        @Test
        void productDelete_evictsBothCaches() throws InterruptedException {
            // arrange
            Product product = createAndSaveProduct("삭제될 상품");
            Long productId = product.getId();
            waitForProductViewCreation(productId);
            
            // 캐시 웜업
            productCacheService.getProductView(productId);
            waitForAsyncProcessing();
            
            // 캐시 존재 확인
            Object cachedInfo = productCacheRedisTemplate.opsForValue().get("product:info:" + productId);
            Object cachedStat = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedInfo).isNotNull();
            assertThat(cachedStat).isNotNull();
            
            // act - 상품 삭제 (이벤트 발행)
            productService.deleteProduct(productId);
            waitForAsyncProcessing();
            
            // assert - Info와 Stat 캐시가 모두 삭제됨
            Object cachedInfoAfterDelete = productCacheRedisTemplate.opsForValue().get("product:info:" + productId);
            Object cachedStatAfterDelete = productCacheRedisTemplate.opsForHash().get("product:stat:" + productId, "likeCount");
            assertThat(cachedInfoAfterDelete).isNull();
            assertThat(cachedStatAfterDelete).isNull();
        }
    }

    // 테스트 헬퍼 메서드
    private Product createAndSaveProduct(String name) {
        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        return productService.createProduct(product).orElseThrow();
    }

    /**
     * 비동기 이벤트 핸들러 완료 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        Thread.sleep(200); // @Async 메서드 완료 대기
    }

    /**
     * ProductView가 생성될 때까지 대기하는 헬퍼 메서드
     * 비동기 이벤트 핸들러가 완료될 때까지 폴링
     */
    private void waitForProductViewCreation(Long productId) {
        int maxAttempts = 50; // 최대 5초 대기 (100ms * 50)
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                if (productViewRepository.findById(productId).isPresent()) {
                    return; // ProductView가 생성되었음
                }
                Thread.sleep(100); // 100ms 대기
                attempt++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ProductView 생성 대기 중 인터럽트 발생", e);
            }
        }
        
        throw new RuntimeException("ProductView 생성 대기 시간 초과: productId=" + productId);
    }

    /**
     * 리플렉션을 사용하여 BaseEntity의 id 필드를 설정하는 헬퍼 메서드
     */
    private void setIdUsingReflection(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID using reflection", e);
        }
    }
}

