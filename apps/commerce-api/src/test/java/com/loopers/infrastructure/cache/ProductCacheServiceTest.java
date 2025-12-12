package com.loopers.infrastructure.cache;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisTestContainersConfig.class)
@DisplayName("ProductCacheService 테스트")
class ProductCacheServiceTest {

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("상품 상세 캐시")
    class ProductDetailCache {

        @Test
        @DisplayName("캐시가 없으면 빈 Optional을 반환한다")
        void getProductDetail_cacheMiss_returnsEmpty() {
            // given
            Long productId = 1L;
            String userId = "user123";

            // when
            Optional<ProductDetailInfo> result = productCacheService.getProductDetail(productId, userId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("캐시에 저장하고 조회할 수 있다")
        void cacheProductDetail_thenGet_returnsValue() {
            // given
            Long productId = 1L;
            String userId = "user123";
            ProductDetailInfo info = new ProductDetailInfo(
                    productId,
                    "테스트 상품",
                    "상품 설명",
                    "http://image.url",
                    10L,
                    "테스트 브랜드",
                    100,
                    true
            );

            // when
            productCacheService.cacheProductDetail(productId, userId, info);
            Optional<ProductDetailInfo> result = productCacheService.getProductDetail(productId, userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().productId()).isEqualTo(productId);
            assertThat(result.get().productName()).isEqualTo("테스트 상품");
            assertThat(result.get().likeCount()).isEqualTo(100);
            assertThat(result.get().likedByUser()).isTrue();
        }

        @Test
        @DisplayName("사용자별로 다른 캐시를 저장한다")
        void cacheProductDetail_differentUsers_separateCaches() {
            // given
            Long productId = 1L;
            String user1 = "user1";
            String user2 = "user2";

            ProductDetailInfo infoForUser1 = new ProductDetailInfo(
                    productId, "상품", "설명", "url", 10L, "브랜드", 100, true
            );
            ProductDetailInfo infoForUser2 = new ProductDetailInfo(
                    productId, "상품", "설명", "url", 10L, "브랜드", 100, false
            );

            // when
            productCacheService.cacheProductDetail(productId, user1, infoForUser1);
            productCacheService.cacheProductDetail(productId, user2, infoForUser2);

            Optional<ProductDetailInfo> resultUser1 = productCacheService.getProductDetail(productId, user1);
            Optional<ProductDetailInfo> resultUser2 = productCacheService.getProductDetail(productId, user2);

            // then
            assertThat(resultUser1.get().likedByUser()).isTrue();
            assertThat(resultUser2.get().likedByUser()).isFalse();
        }

        @Test
        @DisplayName("userId가 null이면 anonymous로 처리한다")
        void cacheProductDetail_nullUserId_treatedAsAnonymous() {
            // given
            Long productId = 1L;
            ProductDetailInfo info = new ProductDetailInfo(
                    productId, "상품", "설명", "url", 10L, "브랜드", 100, false
            );

            // when
            productCacheService.cacheProductDetail(productId, null, info);
            Optional<ProductDetailInfo> result = productCacheService.getProductDetail(productId, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().likedByUser()).isFalse();
        }

        @Test
        @DisplayName("특정 상품의 캐시를 무효화할 수 있다")
        void evictProductDetail_removesCache() {
            // given
            Long productId = 1L;
            String userId = "user123";
            ProductDetailInfo info = new ProductDetailInfo(
                    productId, "상품", "설명", "url", 10L, "브랜드", 100, true
            );
            productCacheService.cacheProductDetail(productId, userId, info);

            // when
            productCacheService.evictProductDetail(productId);
            Optional<ProductDetailInfo> result = productCacheService.getProductDetail(productId, userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("상품 목록 캐시")
    class ProductListCache {

        @Test
        @DisplayName("캐시가 없으면 빈 Optional을 반환한다")
        void getProductList_cacheMiss_returnsEmpty() {
            // given
            String sortType = "LATEST";
            Long brandId = 1L;

            // when
            Optional<List<ProductDetailInfo>> result = productCacheService.getProductList(sortType, brandId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("캐시에 저장하고 조회할 수 있다")
        void cacheProductList_thenGet_returnsValue() {
            // given
            String sortType = "LATEST";
            Long brandId = 1L;
            List<ProductDetailInfo> list = List.of(
                    new ProductDetailInfo(1L, "상품1", "설명1", "url1", brandId, "브랜드", 100, false),
                    new ProductDetailInfo(2L, "상품2", "설명2", "url2", brandId, "브랜드", 50, false)
            );

            // when
            productCacheService.cacheProductList(sortType, brandId, list);
            Optional<List<ProductDetailInfo>> result = productCacheService.getProductList(sortType, brandId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(2);
            assertThat(result.get().get(0).productName()).isEqualTo("상품1");
        }

        @Test
        @DisplayName("정렬 타입별로 다른 캐시를 저장한다")
        void cacheProductList_differentSortTypes_separateCaches() {
            // given
            Long brandId = 1L;
            List<ProductDetailInfo> latestList = List.of(
                    new ProductDetailInfo(1L, "최신상품", "설명", "url", brandId, "브랜드", 10, false)
            );
            List<ProductDetailInfo> likesList = List.of(
                    new ProductDetailInfo(2L, "인기상품", "설명", "url", brandId, "브랜드", 100, false)
            );

            // when
            productCacheService.cacheProductList("LATEST", brandId, latestList);
            productCacheService.cacheProductList("LIKES_DESC", brandId, likesList);

            Optional<List<ProductDetailInfo>> latestResult = productCacheService.getProductList("LATEST", brandId);
            Optional<List<ProductDetailInfo>> likesResult = productCacheService.getProductList("LIKES_DESC", brandId);

            // then
            assertThat(latestResult.get().get(0).productName()).isEqualTo("최신상품");
            assertThat(likesResult.get().get(0).productName()).isEqualTo("인기상품");
        }

        @Test
        @DisplayName("브랜드별로 다른 캐시를 저장한다")
        void cacheProductList_differentBrands_separateCaches() {
            // given
            String sortType = "LATEST";
            Long brand1 = 1L;
            Long brand2 = 2L;
            List<ProductDetailInfo> brand1List = List.of(
                    new ProductDetailInfo(1L, "브랜드1상품", "설명", "url", brand1, "브랜드1", 10, false)
            );
            List<ProductDetailInfo> brand2List = List.of(
                    new ProductDetailInfo(2L, "브랜드2상품", "설명", "url", brand2, "브랜드2", 20, false)
            );

            // when
            productCacheService.cacheProductList(sortType, brand1, brand1List);
            productCacheService.cacheProductList(sortType, brand2, brand2List);

            Optional<List<ProductDetailInfo>> brand1Result = productCacheService.getProductList(sortType, brand1);
            Optional<List<ProductDetailInfo>> brand2Result = productCacheService.getProductList(sortType, brand2);

            // then
            assertThat(brand1Result.get().get(0).productName()).isEqualTo("브랜드1상품");
            assertThat(brand2Result.get().get(0).productName()).isEqualTo("브랜드2상품");
        }

        @Test
        @DisplayName("brandId가 null이면 전체 목록으로 처리한다")
        void cacheProductList_nullBrandId_treatedAsAll() {
            // given
            String sortType = "LATEST";
            List<ProductDetailInfo> allList = List.of(
                    new ProductDetailInfo(1L, "상품1", "설명", "url", 1L, "브랜드1", 10, false),
                    new ProductDetailInfo(2L, "상품2", "설명", "url", 2L, "브랜드2", 20, false)
            );

            // when
            productCacheService.cacheProductList(sortType, null, allList);
            Optional<List<ProductDetailInfo>> result = productCacheService.getProductList(sortType, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(2);
        }

        @Test
        @DisplayName("상품 목록 캐시를 전체 무효화할 수 있다")
        void evictProductList_removesAllCaches() {
            // given
            List<ProductDetailInfo> list = List.of(
                    new ProductDetailInfo(1L, "상품", "설명", "url", 1L, "브랜드", 10, false)
            );
            productCacheService.cacheProductList("LATEST", 1L, list);
            productCacheService.cacheProductList("LIKES_DESC", 2L, list);

            // when
            productCacheService.evictProductList();
            Optional<List<ProductDetailInfo>> result1 = productCacheService.getProductList("LATEST", 1L);
            Optional<List<ProductDetailInfo>> result2 = productCacheService.getProductList("LIKES_DESC", 2L);

            // then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
        }
    }
}
