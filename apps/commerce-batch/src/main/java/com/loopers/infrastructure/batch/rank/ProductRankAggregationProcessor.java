package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.rank.ProductRank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ProductRank 집계를 위한 Processor.
 * <p>
 * 기간 정보를 관리하고 Writer에서 사용할 수 있도록 제공합니다.
 * 실제 집계는 Writer에서 Chunk 단위로 수행됩니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
public class ProductRankAggregationProcessor {

    private ProductRank.PeriodType periodType;
    private LocalDate periodStartDate;

    /**
     * 기간 정보를 설정합니다.
     * <p>
     * Job 파라미터에서 주입받아 설정합니다.
     * </p>
     *
     * @param periodType 기간 타입 (WEEKLY 또는 MONTHLY)
     * @param targetDate 기준 날짜
     */
    public void setPeriod(ProductRank.PeriodType periodType, LocalDate targetDate) {
        this.periodType = periodType;
        
        if (periodType == ProductRank.PeriodType.WEEKLY) {
            // 주간 시작일: 해당 주의 월요일
            this.periodStartDate = targetDate.with(java.time.DayOfWeek.MONDAY);
        } else if (periodType == ProductRank.PeriodType.MONTHLY) {
            // 월간 시작일: 해당 월의 1일
            this.periodStartDate = targetDate.with(TemporalAdjusters.firstDayOfMonth());
        }
    }

    /**
     * 기간 타입을 반환합니다.
     *
     * @return 기간 타입
     */
    public ProductRank.PeriodType getPeriodType() {
        return periodType;
    }

    /**
     * 기간 시작일을 반환합니다.
     *
     * @return 기간 시작일
     */
    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

}

