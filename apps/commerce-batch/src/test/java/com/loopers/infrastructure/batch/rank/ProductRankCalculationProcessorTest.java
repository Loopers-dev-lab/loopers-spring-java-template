package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ProductRankCalculationProcessor 테스트.
 */
@ExtendWith(MockitoExtension.class)
class ProductRankCalculationProcessorTest {

    @Mock
    private ProductRankAggregationProcessor productRankAggregationProcessor;

    private ProductRankCalculationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ProductRankCalculationProcessor(productRankAggregationProcessor);
    }

    @DisplayName("랭킹 번호를 1부터 순차적으로 부여한다")
    @Test
    void assignsRankSequentially() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        ProductRankScore score1 = createProductRankScore(1L, 10L, 20L, 5L);
        ProductRankScore score2 = createProductRankScore(2L, 15L, 25L, 8L);
        ProductRankScore score3 = createProductRankScore(3L, 8L, 15L, 3L);

        // act
        ProductRank rank1 = processor.process(score1);
        ProductRank rank2 = processor.process(score2);
        ProductRank rank3 = processor.process(score3);

        // assert
        assertThat(rank1).isNotNull();
        assertThat(rank1.getRank()).isEqualTo(1);
        assertThat(rank1.getProductId()).isEqualTo(1L);

        assertThat(rank2).isNotNull();
        assertThat(rank2.getRank()).isEqualTo(2);
        assertThat(rank2.getProductId()).isEqualTo(2L);

        assertThat(rank3).isNotNull();
        assertThat(rank3.getRank()).isEqualTo(3);
        assertThat(rank3.getProductId()).isEqualTo(3L);
    }

    @DisplayName("TOP 100에 포함되는 경우 ProductRank를 반환한다")
    @Test
    void returnsProductRankForTop100() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        ProductRankScore score = createProductRankScore(1L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getRank()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getPeriodType()).isEqualTo(periodType);
        assertThat(result.getPeriodStartDate()).isEqualTo(periodStartDate);
        assertThat(result.getLikeCount()).isEqualTo(10L);
        assertThat(result.getSalesCount()).isEqualTo(20L);
        assertThat(result.getViewCount()).isEqualTo(5L);
    }

    @DisplayName("101번째 이후는 null을 반환한다 (TOP 100 초과)")
    @Test
    void returnsNullAfter100th() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        // 100개까지 처리
        for (int i = 1; i <= 100; i++) {
            ProductRankScore score = createProductRankScore((long) i, 10L, 20L, 5L);
            ProductRank result = processor.process(score);
            assertThat(result).isNotNull();
            assertThat(result.getRank()).isEqualTo(i);
        }

        // 101번째 처리 (rank > TOP_RANK_LIMIT이므로 null 반환)
        ProductRankScore score101 = createProductRankScore(101L, 10L, 20L, 5L);
        ProductRank result = processor.process(score101);
        
        // assert
        assertThat(result).isNull(); // TOP 100 초과이므로 null 반환
    }

    @DisplayName("정확히 100번째는 ProductRank를 반환한다")
    @Test
    void returnsProductRankFor100th() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        // 99개까지 처리
        for (int i = 1; i <= 99; i++) {
            ProductRankScore score = createProductRankScore((long) i, 10L, 20L, 5L);
            processor.process(score);
        }

        // 100번째 처리
        ProductRankScore score100 = createProductRankScore(100L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score100);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getRank()).isEqualTo(100);
        assertThat(result.getProductId()).isEqualTo(100L);
    }

    @DisplayName("기간 정보가 설정되지 않으면 null을 반환한다")
    @Test
    void returnsNullWhenPeriodNotSet() throws Exception {
        // arrange
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(null);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(null);

        ProductRankScore score = createProductRankScore(1L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNull();
    }

    @DisplayName("기간 시작일이 설정되지 않으면 null을 반환한다")
    @Test
    void returnsNullWhenPeriodStartDateNotSet() throws Exception {
        // arrange
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(ProductRank.PeriodType.WEEKLY);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(null);

        ProductRankScore score = createProductRankScore(1L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNull();
    }

    @DisplayName("주간 기간 정보로 ProductRank를 생성한다")
    @Test
    void createsProductRankWithWeeklyPeriod() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        ProductRankScore score = createProductRankScore(1L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getPeriodType()).isEqualTo(ProductRank.PeriodType.WEEKLY);
        assertThat(result.getPeriodStartDate()).isEqualTo(periodStartDate);
    }

    @DisplayName("월간 기간 정보로 ProductRank를 생성한다")
    @Test
    void createsProductRankWithMonthlyPeriod() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.MONTHLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 1);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        ProductRankScore score = createProductRankScore(1L, 10L, 20L, 5L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getPeriodType()).isEqualTo(ProductRank.PeriodType.MONTHLY);
        assertThat(result.getPeriodStartDate()).isEqualTo(periodStartDate);
    }

    @DisplayName("ProductRankScore의 메트릭 값을 ProductRank에 전달한다")
    @Test
    void transfersMetricsFromScoreToRank() throws Exception {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9);
        
        when(productRankAggregationProcessor.getPeriodType()).thenReturn(periodType);
        when(productRankAggregationProcessor.getPeriodStartDate()).thenReturn(periodStartDate);

        ProductRankScore score = createProductRankScore(1L, 100L, 200L, 50L);

        // act
        ProductRank result = processor.process(score);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getLikeCount()).isEqualTo(100L);
        assertThat(result.getSalesCount()).isEqualTo(200L);
        assertThat(result.getViewCount()).isEqualTo(50L);
    }

    /**
     * 테스트용 ProductRankScore를 생성합니다.
     */
    private ProductRankScore createProductRankScore(Long productId, Long likeCount, Long salesCount, Long viewCount) {
        double score = likeCount * 0.3 + salesCount * 0.5 + viewCount * 0.2;
        return new ProductRankScore(productId, likeCount, salesCount, viewCount, score);
    }
}

