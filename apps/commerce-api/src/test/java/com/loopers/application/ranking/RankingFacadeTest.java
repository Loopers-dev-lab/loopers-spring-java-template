package com.loopers.application.ranking;

import com.loopers.application.ranking.RankingInfo.RankingItemInfo;
import com.loopers.application.ranking.RankingInfo.RankingPageInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.infrastructure.cache.ProductRankingCache;
import com.loopers.infrastructure.cache.ProductRankingCache.RankingEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingFacadeTest {

    @Mock
    private ProductRankingCache productRankingCache;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private RankingFacade rankingFacade;

    private static final String TODAY_DATE = "20251226";

    @Test
    @DisplayName("getRankings - 상품 정보가 Aggregation 된다")
    void getRankings_aggregatesProductInfo() {
        // given
        List<RankingEntry> rankingEntries = List.of(
            new RankingEntry(1, 100L, 50.0),
            new RankingEntry(2, 200L, 40.0)
        );

        Product product1 = createProduct(100L, "상품A", 1L, 10000);
        Product product2 = createProduct(200L, "상품B", 2L, 20000);
        Brand brand1 = createBrand(1L, "브랜드A");
        Brand brand2 = createBrand(2L, "브랜드B");

        when(productRankingCache.getTodayDate()).thenReturn(TODAY_DATE);
        when(productRankingCache.getTopRankings(TODAY_DATE, 0, 10)).thenReturn(rankingEntries);
        when(productRankingCache.getTotalCount(TODAY_DATE)).thenReturn(2L);
        when(productRepository.findByIdIn(List.of(100L, 200L))).thenReturn(List.of(product1, product2));
        when(brandRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(brand1, brand2));

        // when
        RankingPageInfo result = rankingFacade.getRankings(null, 0, 10);

        // then
        assertThat(result.rankings()).hasSize(2);

        RankingItemInfo first = result.rankings().get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.productId()).isEqualTo(100L);
        assertThat(first.productName()).isEqualTo("상품A");
        assertThat(first.brandName()).isEqualTo("브랜드A");
        assertThat(first.score()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getRankings - 존재하지 않는 상품은 필터링된다")
    void getRankings_filtersNonExistentProducts() {
        // given
        List<RankingEntry> rankingEntries = List.of(
            new RankingEntry(1, 100L, 50.0),
            new RankingEntry(2, 999L, 40.0)  // 존재하지 않는 상품
        );

        Product product1 = createProduct(100L, "상품A", 1L, 10000);
        Brand brand1 = createBrand(1L, "브랜드A");

        when(productRankingCache.getTodayDate()).thenReturn(TODAY_DATE);
        when(productRankingCache.getTopRankings(TODAY_DATE, 0, 10)).thenReturn(rankingEntries);
        when(productRankingCache.getTotalCount(TODAY_DATE)).thenReturn(2L);
        when(productRepository.findByIdIn(anyList())).thenReturn(List.of(product1));
        when(brandRepository.findByIdIn(anyList())).thenReturn(List.of(brand1));

        // when
        RankingPageInfo result = rankingFacade.getRankings(null, 0, 10);

        // then
        assertThat(result.rankings()).hasSize(1);
        assertThat(result.rankings().get(0).productId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getRankings - 날짜 미지정 시 오늘 날짜 사용")
    void getRankings_usesTodayDateWhenNotSpecified() {
        // given
        when(productRankingCache.getTodayDate()).thenReturn(TODAY_DATE);
        when(productRankingCache.getTopRankings(TODAY_DATE, 0, 10)).thenReturn(Collections.emptyList());

        // when
        RankingPageInfo result = rankingFacade.getRankings(null, 0, 10);

        // then
        assertThat(result.date()).isEqualTo(TODAY_DATE);
        verify(productRankingCache).getTopRankings(eq(TODAY_DATE), anyInt(), anyInt());
    }

    @Test
    @DisplayName("getRankings - 페이지 정보가 정확하다")
    void getRankings_pageInfoIsCorrect() {
        // given - 첫 페이지에 10개 상품이 있고, 전체 25개
        List<RankingEntry> rankingEntries = List.of(
            new RankingEntry(1, 100L, 50.0)
        );
        Product product = createProduct(100L, "상품A", 1L, 10000);
        Brand brand = createBrand(1L, "브랜드A");

        when(productRankingCache.getTodayDate()).thenReturn(TODAY_DATE);
        when(productRankingCache.getTopRankings(TODAY_DATE, 0, 10)).thenReturn(rankingEntries);
        when(productRankingCache.getTotalCount(TODAY_DATE)).thenReturn(25L);
        when(productRepository.findByIdIn(anyList())).thenReturn(List.of(product));
        when(brandRepository.findByIdIn(anyList())).thenReturn(List.of(brand));

        // when
        RankingPageInfo result = rankingFacade.getRankings(null, 0, 10);

        // then
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalCount()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);  // ceil(25/10)
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("getRankings - 빈 결과 시 빈 리스트 반환")
    void getRankings_returnsEmptyListWhenNoResults() {
        // given
        when(productRankingCache.getTodayDate()).thenReturn(TODAY_DATE);
        when(productRankingCache.getTopRankings(TODAY_DATE, 0, 10)).thenReturn(Collections.emptyList());

        // when
        RankingPageInfo result = rankingFacade.getRankings(null, 0, 10);

        // then
        assertThat(result.rankings()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.hasNext()).isFalse();
    }

    private Product createProduct(Long id, String name, Long brandId, int price) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getBrandId()).thenReturn(brandId);
        when(product.getPrice()).thenReturn(Money.of(price));
        when(product.getLikeCount()).thenReturn(10);
        return product;
    }

    private Brand createBrand(Long id, String name) {
        Brand brand = mock(Brand.class);
        when(brand.getId()).thenReturn(id);
        when(brand.getName()).thenReturn(name);
        return brand;
    }

    @Test
    @DisplayName("getRankings - 잘못된 날짜 형식은 예외 발생")
    void getRankings_throwsExceptionForInvalidDateFormat() {
        // given
        String invalidDate = "2025-12-26";  // yyyy-MM-dd 형식 (잘못됨)

        // when & then
        assertThatThrownBy(() -> rankingFacade.getRankings(invalidDate, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }

    @Test
    @DisplayName("getRankings - 존재하지 않는 날짜는 예외 발생")
    void getRankings_throwsExceptionForNonExistentDate() {
        // given
        String invalidDate = "20251332";  // 13월 32일 (존재하지 않는 날짜)

        // when & then
        assertThatThrownBy(() -> rankingFacade.getRankings(invalidDate, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }
}
