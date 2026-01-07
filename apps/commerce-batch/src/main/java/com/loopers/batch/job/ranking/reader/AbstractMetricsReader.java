package com.loopers.batch.job.ranking.reader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.batch.job.ranking.support.RankingAggregator;
import com.loopers.domain.metrics.ProductMetricsAggregation;
import com.loopers.domain.metrics.ProductMetricsRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭 Reader 공통 추상 클래스
 * - 특정 기간의 데이터를 집계하고 랭킹을 생성하는 공통 로직을 포함
 */
@Slf4j
public abstract class AbstractMetricsReader implements ItemReader<RankingAggregation> {

    protected final ProductMetricsRepository productMetricsRepository;
    protected final RankingAggregator rankingAggregator;

    private Iterator<RankingAggregation> iterator;

    protected AbstractMetricsReader(ProductMetricsRepository productMetricsRepository, RankingAggregator rankingAggregator) {
        this.productMetricsRepository = productMetricsRepository;
        this.rankingAggregator = rankingAggregator;
    }

    @Override
    public RankingAggregation read() throws Exception {
        if (iterator == null) {
            initializeIterator();
        }

        return iterator.hasNext() ? iterator.next() : null;
    }

    private void initializeIterator() {
        String logIdentifier = getLogIdentifier();
        log.info("{} 랭킹 집계 시작: parameter={}", logIdentifier, getParameterValue());

        try {
            // 1. 기간 파싱 (추상 메서드 호출)
            LocalDate[] dateRange = parseDateRange();
            if (dateRange == null || dateRange.length != 2) {
                throw new IllegalStateException("parseDateRange()는 정확히 2개의 날짜를 반환해야 합니다.");
            }
            LocalDate startDate = dateRange[0];
            LocalDate endDate = dateRange[1];

            log.info("집계 기간: {} ~ {}", startDate, endDate);

            // 2. DB에서 집계 쿼리 실행
            List<ProductMetricsAggregation> aggregationResults = productMetricsRepository.aggregateByDateRange(startDate, endDate);
            log.info("집계 대상 상품 수: {}", aggregationResults.size());

            // 3. 랭킹 처리 (정렬 + TOP 100 + 순위 부여)
            List<RankingAggregation> rankings = rankingAggregator.processRankings(aggregationResults);
            log.info("생성된 랭킹 수: {}", rankings.size());

            iterator = rankings.iterator();

        } catch (Exception e) {
            log.error("{} 랭킹 집계 중 오류 발생: parameter={}", logIdentifier, getParameterValue(), e);
            throw new RuntimeException(logIdentifier + " 랭킹 집계 실패", e);
        }
    }

    /**
     * 기간에 해당하는 LocalDate 범위를 반환합니다.
     */
    protected abstract LocalDate[] parseDateRange();

    /**
     * 로그 식별자를 반환합니다. (예: "월간", "주간")
     */
    protected abstract String getLogIdentifier();

    /**
     * 현재 사용 중인 파라미터 값을 반환합니다.
     */
    protected abstract String getParameterValue();
}
