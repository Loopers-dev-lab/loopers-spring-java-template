package com.loopers.batch.job.ranking.reader;

import com.loopers.batch.domain.RankingWeightReader;
import com.loopers.dto.ProductMetricsSummary;
import com.loopers.dto.RankedProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@StepScope
@Component
public class MonthlyRankingReader implements ItemReader<RankedProduct> {

    private final ProductMetricsJpaRepository metricsRepository;
    private final RankingWeightReader rankingWeightReader;
    private Iterator<RankedProduct> iterator;
    private boolean initialized = false;

    @Value("#{jobParameters['yearMonth']}")
    private String yearMonthStr;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_N = 100;

    public MonthlyRankingReader(ProductMetricsJpaRepository metricsRepository,
                                RankingWeightReader rankingWeightReader) {
        this.metricsRepository = metricsRepository;
        this.rankingWeightReader = rankingWeightReader;
    }

    @Override
    public RankedProduct read() {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private void initialize() {
        YearMonth yearMonth = parseYearMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        log.info("월간 랭킹 Reader 초기화: yearMonth={}, startDate={}, endDate={}", yearMonth, startDate, endDate);

        List<ProductMetricsSummary> summaries = metricsRepository.findAllByProductIdAndDateBetween(startDate, endDate);

        if (summaries.isEmpty()) {
            log.warn("집계할 메트릭 데이터가 없습니다.");
            iterator = List.<RankedProduct>of().iterator();
            return;
        }

        // 상품별 집계
        Map<Long, double[]> aggregatedMap = summaries.stream()
                .collect(Collectors.groupingBy(
                        ProductMetricsSummary::getProductId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    double[] sums = new double[3];
                                    for (ProductMetricsSummary s : list) {
                                        sums[0] += s.getLikeCount();
                                        sums[1] += s.getStockCount();
                                        sums[2] += s.getViewCount();
                                    }
                                    return sums;
                                }
                        )
                ));

        // 점수 계산 및 정렬
        List<RankedProduct> rankedProducts = aggregatedMap.entrySet().stream()
                .map(entry -> {
                    double[] sums = entry.getValue();
                    double score = rankingWeightReader.calculateTotalScore(
                            (int) sums[0], (int) sums[1], (int) sums[2]
                    );
                    return new RankedProduct(entry.getKey(), score);
                })
                .sorted(Comparator.comparingDouble(RankedProduct::score).reversed())
                .limit(TOP_N)
                .toList();

        log.info("월간 랭킹 Reader 완료: 집계 상품 수={}, TOP-N={}", aggregatedMap.size(), rankedProducts.size());

        iterator = rankedProducts.iterator();
    }

    private YearMonth parseYearMonth() {
        if (yearMonthStr != null && !yearMonthStr.isBlank()) {
            return YearMonth.parse(yearMonthStr, YEAR_MONTH_FORMATTER);
        }
        // 기본값: 이번 달
        return YearMonth.now();
    }
}
