package com.loopers.application.product;

import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.service.LikeReadService;
import com.loopers.domain.product.service.ProductReadService;
import com.loopers.domain.product.vo.Stock;
import com.loopers.infrastructure.cache.ProductDetailCache;
import com.loopers.infrastructure.cache.ProductListCache;
import com.loopers.infrastructure.cache.ProductRankingCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @Mock
    private ProductReadService productReadService;

    @Mock
    private LikeReadService likeReadService;

    @Mock
    private ProductDetailCache productDetailCache;

    @Mock
    private ProductListCache productListCache;

    @Mock
    private ProductRankingCache productRankingCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductFacade productFacade;

    private static final Long PRODUCT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long BRAND_ID = 10L;

    private ProductDetailInfo createCachedProductDetailInfo() {
        return ProductDetailInfo.builder()
                .id(PRODUCT_ID)
                .name("테스트 상품")
                .description("테스트 설명")
                .brandId(BRAND_ID)
                .brandName("테스트 브랜드")
                .brandDescription("브랜드 설명")
                .price(Money.of(10000))
                .stock(Stock.of(100))
                .likeCount(50)
                .isLikedByMember(false)
                .ranking(null)
                .build();
    }

    @DisplayName("상품 상세 조회 (getProductDetail)")
    @Nested
    class GetProductDetail {

        @DisplayName("랭킹에 존재하는 상품이면 순위가 포함되어 반환된다")
        @Test
        void shouldIncludeRanking_whenProductIsRanked() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(5);  // 5위

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            assertThat(result.getRanking()).isEqualTo(5);
            verify(productRankingCache).getRank(PRODUCT_ID);
        }

        @DisplayName("랭킹에 존재하지 않는 상품이면 순위가 null로 반환된다")
        @Test
        void shouldReturnNullRanking_whenProductIsNotRanked() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(null);  // 순위권 밖

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            assertThat(result.getRanking()).isNull();
            verify(productRankingCache).getRank(PRODUCT_ID);
        }

        @DisplayName("랭킹 1위 상품이면 순위가 1로 반환된다")
        @Test
        void shouldReturnRanking1_whenProductIsFirstPlace() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(true);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(1);  // 1위

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            assertThat(result.getRanking()).isEqualTo(1);
            assertThat(result.isLikedByMember()).isTrue();
        }

        @DisplayName("비로그인 사용자도 랭킹 정보가 포함되어 반환된다")
        @Test
        void shouldIncludeRanking_whenUserNotLoggedIn() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(10);  // 10위

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, null);

            // then
            assertThat(result.getRanking()).isEqualTo(10);
            assertThat(result.isLikedByMember()).isFalse();
            verify(likeReadService, never()).isLikedBy(anyLong(), anyLong());
        }

        @DisplayName("캐시 miss 시 DB 조회 후에도 랭킹 정보가 포함된다")
        @Test
        void shouldIncludeRanking_whenCacheMiss() {
            // given
            ProductDetailInfo dbInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.empty());  // 캐시 miss
            when(productReadService.getProductDetail(PRODUCT_ID, null)).thenReturn(dbInfo);
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(3);

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            assertThat(result.getRanking()).isEqualTo(3);
            verify(productDetailCache).set(PRODUCT_ID, dbInfo);  // 캐시 저장 확인
            verify(productRankingCache).getRank(PRODUCT_ID);
        }

        @DisplayName("상품 조회 이벤트가 발행된다")
        @Test
        void shouldPublishProductViewedEvent() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(5);

            // when
            productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            ArgumentCaptor<ProductViewedEvent> eventCaptor = ArgumentCaptor.forClass(ProductViewedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ProductViewedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.productId()).isEqualTo(PRODUCT_ID);
            assertThat(capturedEvent.memberId()).isEqualTo(MEMBER_ID);
            assertThat(capturedEvent.brandId()).isEqualTo(BRAND_ID);
        }

        @DisplayName("다른 필드들이 올바르게 유지된다")
        @Test
        void shouldPreserveOtherFields() {
            // given
            ProductDetailInfo cachedInfo = createCachedProductDetailInfo();
            when(productDetailCache.get(PRODUCT_ID)).thenReturn(Optional.of(cachedInfo));
            when(likeReadService.isLikedBy(MEMBER_ID, PRODUCT_ID)).thenReturn(true);
            when(productRankingCache.getRank(PRODUCT_ID)).thenReturn(7);

            // when
            ProductDetailInfo result = productFacade.getProductDetail(PRODUCT_ID, MEMBER_ID);

            // then
            assertThat(result.getId()).isEqualTo(PRODUCT_ID);
            assertThat(result.getName()).isEqualTo("테스트 상품");
            assertThat(result.getDescription()).isEqualTo("테스트 설명");
            assertThat(result.getBrandId()).isEqualTo(BRAND_ID);
            assertThat(result.getBrandName()).isEqualTo("테스트 브랜드");
            assertThat(result.getBrandDescription()).isEqualTo("브랜드 설명");
            assertThat(result.getPrice().getAmount().intValue()).isEqualTo(10000);
            assertThat(result.getStock().getQuantity()).isEqualTo(100);
            assertThat(result.getLikeCount()).isEqualTo(50);
            assertThat(result.isLikedByMember()).isTrue();
            assertThat(result.getRanking()).isEqualTo(7);
        }
    }
}
