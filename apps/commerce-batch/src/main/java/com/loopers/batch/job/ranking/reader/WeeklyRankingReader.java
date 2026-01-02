package com.loopers.batch.job.ranking.reader;

import com.loopers.batch.domain.RankingWeightReader;
import com.loopers.dto.ProductMetricsSummary;
import com.loopers.dto.RankedProduct;
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@StepScope
@Component
public class WeeklyRankingReader implements ItemReader<RankedProduct> {

    private final ProductMetricsJpaRepository metricsRepository;
    private final RankingWeightReader rankingWeightReader;
    private Iterator<RankedProduct> iterator;
    private boolean initialized = false;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int TOP_N = 100;

    public WeeklyRankingReader(ProductMetricsJpaRepository metricsRepository,
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
        LocalDate startDate = parseStartDate();
        LocalDate endDate = parseEndDate();

        log.info("주간 랭킹 Reader 초기화: startDate={}, endDate={}", startDate, endDate);

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

        log.info("주간 랭킹 Reader 완료: 집계 상품 수={}, TOP-N={}", aggregatedMap.size(), rankedProducts.size());

        iterator = rankedProducts.iterator();
    }

    private LocalDate parseStartDate() {
        if (startDateStr != null && !startDateStr.isBlank()) {
            return LocalDate.parse(startDateStr, DATE_FORMATTER);
        }
        // 기본값: 이번 주 월요일
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private LocalDate parseEndDate() {
        if (endDateStr != null && !endDateStr.isBlank()) {
            return LocalDate.parse(endDateStr, DATE_FORMATTER);
        }
        // 기본값: 이번 주 일요일
        return LocalDate.now().with(DayOfWeek.SUNDAY);
    }
}
