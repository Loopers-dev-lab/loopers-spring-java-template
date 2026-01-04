package com.loopers.domain.rank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductRank 도메인 엔티티 테스트.
 * <p>
 * commerce-batch 모듈의 ProductRank 엔티티에 대한 단위 테스트입니다.
 * </p>
 */
class ProductRankTest {

    @DisplayName("ProductRank는 모든 필수 정보로 생성된다")
    @Test
    void createsProductRankWithAllFields() {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        LocalDate periodStartDate = LocalDate.of(2024, 12, 9); // 월요일
        Long productId = 1L;
        Integer rank = 1;
        Long likeCount = 100L;
        Long salesCount = 500L;
        Long viewCount = 1000L;

        // act
        ProductRank productRank = new ProductRank(
            periodType,
            periodStartDate,
            productId,
            rank,
            likeCount,
            salesCount,
            viewCount
        );

        // assert
        assertThat(productRank.getPeriodType()).isEqualTo(periodType);
        assertThat(productRank.getPeriodStartDate()).isEqualTo(periodStartDate);
        assertThat(productRank.getProductId()).isEqualTo(productId);
        assertThat(productRank.getRank()).isEqualTo(rank);
        assertThat(productRank.getLikeCount()).isEqualTo(likeCount);
        assertThat(productRank.getSalesCount()).isEqualTo(salesCount);
        assertThat(productRank.getViewCount()).isEqualTo(viewCount);
        assertThat(productRank.getCreatedAt()).isNotNull();
        assertThat(productRank.getUpdatedAt()).isNotNull();
    }

    @DisplayName("ProductRank 생성 시 createdAt과 updatedAt이 현재 시간으로 설정된다")
    @Test
    void setsCreatedAtAndUpdatedAtOnCreation() throws InterruptedException {
        // arrange
        LocalDateTime beforeCreation = LocalDateTime.now();
        Thread.sleep(1);

        // act
        ProductRank productRank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            LocalDate.of(2024, 12, 9),
            1L,
            1,
            100L,
            500L,
            1000L
        );

        Thread.sleep(1);
        LocalDateTime afterCreation = LocalDateTime.now();

        // assert
        assertThat(productRank.getCreatedAt())
            .isAfter(beforeCreation)
            .isBefore(afterCreation);
        assertThat(productRank.getUpdatedAt())
            .isAfter(beforeCreation)
            .isBefore(afterCreation);
    }

    @DisplayName("주간 랭킹을 생성할 수 있다")
    @Test
    void createsWeeklyRank() {
        // arrange
        LocalDate weekStart = LocalDate.of(2024, 12, 9); // 월요일

        // act
        ProductRank weeklyRank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            weekStart,
            1L,
            1,
            100L,
            500L,
            1000L
        );

        // assert
        assertThat(weeklyRank.getPeriodType()).isEqualTo(ProductRank.PeriodType.WEEKLY);
        assertThat(weeklyRank.getPeriodStartDate()).isEqualTo(weekStart);
    }

    @DisplayName("월간 랭킹을 생성할 수 있다")
    @Test
    void createsMonthlyRank() {
        // arrange
        LocalDate monthStart = LocalDate.of(2024, 12, 1); // 월의 1일

        // act
        ProductRank monthlyRank = new ProductRank(
            ProductRank.PeriodType.MONTHLY,
            monthStart,
            1L,
            1,
            100L,
            500L,
            1000L
        );

        // assert
        assertThat(monthlyRank.getPeriodType()).isEqualTo(ProductRank.PeriodType.MONTHLY);
        assertThat(monthlyRank.getPeriodStartDate()).isEqualTo(monthStart);
    }

    @DisplayName("랭킹 정보를 업데이트할 수 있다")
    @Test
    void canUpdateRank() throws InterruptedException {
        // arrange
        ProductRank productRank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            LocalDate.of(2024, 12, 9),
            1L,
            1,
            100L,
            500L,
            1000L
        );
        Integer newRank = 2;
        Long newLikeCount = 200L;
        Long newSalesCount = 600L;
        Long newViewCount = 1100L;
        LocalDateTime initialUpdatedAt = productRank.getUpdatedAt();

        // act
        Thread.sleep(1); // 시간 차이를 보장하기 위한 작은 지연
        productRank.updateRank(newRank, newLikeCount, newSalesCount, newViewCount);

        // assert
        assertThat(productRank.getRank()).isEqualTo(newRank);
        assertThat(productRank.getLikeCount()).isEqualTo(newLikeCount);
        assertThat(productRank.getSalesCount()).isEqualTo(newSalesCount);
        assertThat(productRank.getViewCount()).isEqualTo(newViewCount);
        assertThat(productRank.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @DisplayName("랭킹 업데이트 시 updatedAt이 갱신된다")
    @Test
    void updatesUpdatedAtWhenRankIsUpdated() throws InterruptedException {
        // arrange
        ProductRank productRank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            LocalDate.of(2024, 12, 9),
            1L,
            1,
            100L,
            500L,
            1000L
        );
        LocalDateTime initialUpdatedAt = productRank.getUpdatedAt();

        // act
        Thread.sleep(1);
        productRank.updateRank(2, 200L, 600L, 1100L);

        // assert
        assertThat(productRank.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @DisplayName("PeriodType enum이 올바르게 정의되어 있다")
    @Test
    void periodTypeEnumIsCorrectlyDefined() {
        // assert
        assertThat(ProductRank.PeriodType.WEEKLY).isNotNull();
        assertThat(ProductRank.PeriodType.MONTHLY).isNotNull();
        assertThat(ProductRank.PeriodType.values()).hasSize(2);
    }

    @DisplayName("TOP 100 랭킹을 생성할 수 있다")
    @Test
    void createsTop100Rank() {
        // arrange
        Integer topRank = 100;

        // act
        ProductRank top100Rank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            LocalDate.of(2024, 12, 9),
            100L,
            topRank,
            1L,
            1L,
            1L
        );

        // assert
        assertThat(top100Rank.getRank()).isEqualTo(topRank);
        assertThat(top100Rank.getRank()).isLessThanOrEqualTo(100);
    }

    @DisplayName("랭킹 1위를 생성할 수 있다")
    @Test
    void createsFirstRank() {
        // arrange
        Integer firstRank = 1;

        // act
        ProductRank firstPlaceRank = new ProductRank(
            ProductRank.PeriodType.WEEKLY,
            LocalDate.of(2024, 12, 9),
            1L,
            firstRank,
            1000L,
            5000L,
            10000L
        );

        // assert
        assertThat(firstPlaceRank.getRank()).isEqualTo(firstRank);
        assertThat(firstPlaceRank.getRank()).isGreaterThanOrEqualTo(1);
    }
}

