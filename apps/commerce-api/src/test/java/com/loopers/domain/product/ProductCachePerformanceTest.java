package com.loopers.domain.product;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.*;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductCachePerformanceTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    @Qualifier("redisTemplateCache")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static boolean initialized = false;
    private User testUser;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            redisTemplate.getConnectionFactory()
                    .getConnection()
                    .serverCommands()
                    .flushAll();

            // 테스트용 사용자 생성
            testUser = testFixture.createUser("cacheTestUser");

            System.out.println("✓ Redis 캐시 초기화 완료");
            initialized = true;
        } else {
            // 이미 초기화된 경우 사용자 조회
            testUser = testFixture.createUser("cacheUser" + System.currentTimeMillis());
        }
    }

    @Test
    @DisplayName("상품 상세 조회 시 캐시 HIT/MISS 동작 검증")
    void testProductDetailCacheHitMiss() {
        Long productId = 1L;

        // 1차 조회 - Cache MISS
        long startMiss = System.currentTimeMillis();
        ProductDetailInfo firstCall = productFacade.getProductDetail(productId);
        long missDuration = System.currentTimeMillis() - startMiss;

        // 2차 조회 - Cache HIT
        long startHit = System.currentTimeMillis();
        ProductDetailInfo secondCall = productFacade.getProductDetail(productId);
        long hitDuration = System.currentTimeMillis() - startHit;

        System.out.println("=== 캐시 성능 비교 ===");
        System.out.println("Cache MISS: " + missDuration + "ms");
        System.out.println("Cache HIT: " + hitDuration + "ms");
        System.out.println("성능 향상: " + ((missDuration - hitDuration) / (double) missDuration * 100) + "%");

        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
        assertThat(hitDuration).isLessThan(missDuration);
    }

    @Test
    @DisplayName("상품 목록 조회 시 캐시 적용 효과 검증")
    void testProductListCacheEffect() {
        ProductGetListCommand command = new ProductGetListCommand(
                1L,
                "price_asc",
                PageRequest.of(0, 20)
        );

        // MISS
        long startMiss = System.currentTimeMillis();
        ProductListInfo firstResult = productFacade.getProducts(command);
        long missDuration = System.currentTimeMillis() - startMiss;

        // HIT
        long startHit = System.currentTimeMillis();
        ProductListInfo secondResult = productFacade.getProducts(command);
        long hitDuration = System.currentTimeMillis() - startHit;

        System.out.println("=== 목록 캐시 성능 비교 ===");
        System.out.println("Cache MISS: " + missDuration + "ms");
        System.out.println("Cache HIT: " + hitDuration + "ms");

        assertThat(hitDuration).isLessThan(missDuration);
    }

    @Test
    @DisplayName("좋아요 등록 시 상품 상세 캐시가 무효화되는지 검증")
    void testCacheEvictionOnLikeAdd() {
        Long productId = 1L;
        String cacheKey = "product:v1:detail:" + productId;

        // 캐시 생성
        productFacade.getProductDetail(productId);
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 좋아요 등록 - 실제 사용자의 loginId 사용
        likeFacade.addLike(testUser.getLoginIdValue(), productId);

        // 캐시 무효화 검증
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }
}
