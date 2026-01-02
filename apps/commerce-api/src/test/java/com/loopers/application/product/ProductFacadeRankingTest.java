package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductMVService;
import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.tracking.UserBehaviorTracker;
import com.loopers.domain.user.UserService;

/**
 * ProductFacade 랭킹 관련 기능 단위 테스트
 *
 * @author hyunjikoh
 * @since 2025.12.26
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ProductFacadeRankingTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductMVService mvService;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private LikeService likeService;

    @Mock
    private UserService userService;

    @Mock
    private BrandService brandService;

    @Mock
    private UserBehaviorTracker behaviorTracker;

    @Mock
    private RankingRedisService rankingRedisService;

    @InjectMocks
    private ProductFacade productFacade;

    private ProductMaterializedViewEntity createMockMVEntity(Long productId, String name) {
        ProductMaterializedViewEntity mv = mock(ProductMaterializedViewEntity.class, RETURNS_DEEP_STUBS);
        when(mv.getProductId()).thenReturn(productId);
        when(mv.getName()).thenReturn(name);
        when(mv.getDescription()).thenReturn("Description for " + name);
        when(mv.getLikeCount()).thenReturn(10L);
        when(mv.getStockQuantity()).thenReturn(100);
        when(mv.getBrandId()).thenReturn(1L);
        when(mv.getBrandName()).thenReturn("Test Brand");
        when(mv.getCreatedAt()).thenReturn(java.time.ZonedDateTime.now());

        when(mv.getPrice().getOriginPrice()).thenReturn(BigDecimal.valueOf(10000));
        when(mv.getPrice().getDiscountPrice()).thenReturn(BigDecimal.valueOf(9000));

        return mv;
    }

    @Nested
    @DisplayName("랭킹 상품 목록 조회 테스트")
    class GetRankingProductsTest {

        @Test
        @DisplayName("랭킹 순서대로 상품 목록을 조회해야 한다")
        void shouldReturnProductsInRankingOrder() {
            // Given
            LocalDate today = LocalDate.now();
            Pageable pageable = PageRequest.of(0, 20);

            List<RankingItem> rankings = List.of(
                    new RankingItem(1, 101L, 100.0),
                    new RankingItem(2, 102L, 90.0),
                    new RankingItem(3, 103L, 80.0)
            );

            List<ProductMaterializedViewEntity> mvEntities = List.of(
                    createMockMVEntity(101L, "Product 101"),
                    createMockMVEntity(102L, "Product 102"),
                    createMockMVEntity(103L, "Product 103")
            );

            when(rankingRedisService.getRanking(today, 1, 20)).thenReturn(rankings);
            when(mvService.getByIds(List.of(101L, 102L, 103L))).thenReturn(mvEntities);
            when(rankingRedisService.getRankingCount(today)).thenReturn(3L);

            // When
            Page<ProductInfo> result = productFacade.getRankingProducts(pageable, today);

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).id()).isEqualTo(101L); // 1위
            assertThat(result.getContent().get(1).id()).isEqualTo(102L); // 2위
            assertThat(result.getContent().get(2).id()).isEqualTo(103L); // 3위
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 페이지를 반환해야 한다")
        void shouldReturnEmptyPageWhenNoRankingData() {
            // Given
            LocalDate today = LocalDate.now();
            Pageable pageable = PageRequest.of(0, 20);

            when(rankingRedisService.getRanking(today, 1, 20)).thenReturn(List.of());

            // When
            Page<ProductInfo> result = productFacade.getRankingProducts(pageable, today);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("날짜가 null이면 오늘 날짜를 사용해야 한다")
        void shouldUseTodayWhenDateIsNull() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate today = LocalDate.now();

            when(rankingRedisService.getRanking(eq(today), anyInt(), anyInt())).thenReturn(List.of());

            // When
            productFacade.getRankingProducts(pageable, null);

            // Then
            verify(rankingRedisService).getRanking(eq(today), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("콜드 스타트 Fallback 테스트")
    class ColdStartFallbackTest {

        @Test
        @DisplayName("오늘 랭킹이 비어있으면 어제 랭킹을 조회해야 한다")
        void shouldFallbackToYesterdayWhenTodayIsEmpty() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            Pageable pageable = PageRequest.of(0, 20);

            List<RankingItem> yesterdayRankings = List.of(
                    new RankingItem(1, 201L, 50.0),
                    new RankingItem(2, 202L, 40.0)
            );

            List<ProductMaterializedViewEntity> mvEntities = List.of(
                    createMockMVEntity(201L, "Product 201"),
                    createMockMVEntity(202L, "Product 202")
            );

            // 오늘 랭킹은 비어있음
            when(rankingRedisService.getRanking(today, 1, 20)).thenReturn(List.of());
            // 어제 랭킹은 있음
            when(rankingRedisService.getRanking(yesterday, 1, 20)).thenReturn(yesterdayRankings);
            when(mvService.getByIds(List.of(201L, 202L))).thenReturn(mvEntities);
            when(rankingRedisService.getRankingCount(yesterday)).thenReturn(2L);

            // When - date를 null로 전달 (오늘 날짜 사용)
            Page<ProductInfo> result = productFacade.getRankingProducts(pageable, null);

            // Then - 어제 랭킹이 반환되어야 함
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).id()).isEqualTo(201L);
            assertThat(result.getContent().get(1).id()).isEqualTo(202L);
        }

        @Test
        @DisplayName("명시적 날짜 지정 시 Fallback하지 않아야 한다")
        void shouldNotFallbackWhenDateIsExplicitlySpecified() {
            // Given
            LocalDate specificDate = LocalDate.now().minusDays(5);
            Pageable pageable = PageRequest.of(0, 20);

            when(rankingRedisService.getRanking(specificDate, 1, 20)).thenReturn(List.of());

            // When - 명시적으로 날짜 지정
            Page<ProductInfo> result = productFacade.getRankingProducts(pageable, specificDate);

            // Then - Fallback 없이 빈 결과 반환
            assertThat(result.getContent()).isEmpty();
            // 어제 랭킹 조회 안 함
            verify(rankingRedisService, never()).getRanking(eq(specificDate.minusDays(1)), anyInt(), anyInt());
        }

        @Test
        @DisplayName("오늘과 어제 모두 비어있으면 빈 페이지를 반환해야 한다")
        void shouldReturnEmptyWhenBothTodayAndYesterdayAreEmpty() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            Pageable pageable = PageRequest.of(0, 20);

            when(rankingRedisService.getRanking(today, 1, 20)).thenReturn(List.of());
            when(rankingRedisService.getRanking(yesterday, 1, 20)).thenReturn(List.of());

            // When
            Page<ProductInfo> result = productFacade.getRankingProducts(pageable, null);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 시 랭킹 정보 포함 테스트")
    class ProductDetailWithRankingTest {

        @Test
        @DisplayName("상품 상세 조회 시 랭킹 정보가 포함되어야 한다")
        void shouldIncludeRankingInProductDetail() {
            // Given
            Long productId = 301L;
            LocalDate today = LocalDate.now();

            ProductMaterializedViewEntity mvEntity = createMockMVEntity(productId, "Ranked Product");
            RankingItem ranking = new RankingItem(5, productId, 75.0);

            when(productCacheService.getProductDetailFromCache(productId)).thenReturn(Optional.empty());
            when(mvService.getById(productId)).thenReturn(mvEntity);
            when(rankingRedisService.getProductRanking(today, productId)).thenReturn(ranking);

            // When
            ProductDetailInfo result = productFacade.getProductDetail(productId, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.ranking()).isNotNull();
            assertThat(result.ranking().rank()).isEqualTo(5);
            assertThat(result.ranking().score()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 ranking이 null이어야 한다")
        void shouldHaveNullRankingForUnrankedProduct() {
            // Given
            Long productId = 302L;
            LocalDate today = LocalDate.now();

            ProductMaterializedViewEntity mvEntity = createMockMVEntity(productId, "Unranked Product");

            when(productCacheService.getProductDetailFromCache(productId)).thenReturn(Optional.empty());
            when(mvService.getById(productId)).thenReturn(mvEntity);
            when(rankingRedisService.getProductRanking(today, productId)).thenReturn(null);

            // When
            ProductDetailInfo result = productFacade.getProductDetail(productId, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.ranking()).isNull();
        }
    }
}
