package com.loopers.batch.job.ranking.reader;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.batch.job.ranking.support.DateRangeParser;
import com.loopers.batch.job.ranking.support.RankingAggregator;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

/**
 * 주간 메트릭 Reader
 * - 지정된 주차의 7일간 데이터를 집계
 * - TOP 100 랭킹 생성 및 순위 부여
 */
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class WeeklyMetricsReader implements ItemReader<RankingAggregation> {

    private final ProductMetricsRepository productMetricsRepository;
    private final DateRangeParser dateRangeParser;
    private final RankingAggregator rankingAggregator;
    
    private Iterator<RankingAggregation> iterator;

    @Value("#{jobParameters['yearWeek']}")
    private String yearWeek;  // e.g., "2024-W52"

    @Override
    public RankingAggregation read() throws Exception {
        if (iterator == null) {
            initializeIterator();
        }
        
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void initializeIterator() {
        log.info("주간 랭킹 집계 시작: yearWeek={}", yearWeek);
        
        try {
            // 1. 주차 → 날짜 범위 변환
            LocalDate[] dateRange = dateRangeParser.parseYearWeek(yearWeek);
            LocalDate startDate = dateRange[0];
            LocalDate endDate = dateRange[1];
            
            log.info("집계 기간: {} ~ {}", startDate, endDate);
            
            // 2. DB에서 집계 쿼리 실행
            List<Object[]> aggregationResults = productMetricsRepository.aggregateByDateRange(startDate, endDate);
            log.info("집계 대상 상품 수: {}", aggregationResults.size());
            
            // 3. 랭킹 처리 (정렬 + TOP 100 + 순위 부여)
            List<RankingAggregation> rankings = rankingAggregator.processRankings(aggregationResults);
            log.info("생성된 랭킹 수: {}", rankings.size());
            
            iterator = rankings.iterator();
            
        } catch (Exception e) {
            log.error("주간 랭킹 집계 중 오류 발생: yearWeek={}", yearWeek, e);
            throw new RuntimeException("주간 랭킹 집계 실패", e);
        }
    }
}