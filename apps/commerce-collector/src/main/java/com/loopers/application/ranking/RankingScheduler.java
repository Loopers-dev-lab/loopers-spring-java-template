package com.loopers.application.ranking;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    @Value("${ranking.weight.like:0.2}")
    private double likeWeight;

    @Value("${ranking.weight.view:0.1}")
    private double viewWeight;

    @Value("${ranking.weight.order:0.6}")
    private double orderWeight;

    private final ProductMetricsDailyRepository productMetricsDailyRepository;
    private final RankingFacade rankingFacade;

    /**
     * 모든 메트릭 증감량 처리 (5분마다)
     * - is_processed = false인 레코드만 조회
     * - Redis에 증감량 반영 (ZINCRBY)
     * - 처리 완료 후 is_processed = true로 업데이트
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processUnprocessedDeltas() {
        try {
            log.info("미처리 메트릭 증감 처리 시작");

            LocalDate today = LocalDate.now();
            List<ProductMetricsDaily> unprocessedRecords =
                    productMetricsDailyRepository.findAllByMetricDateAndIsProcessed(today, false);

            if (unprocessedRecords.isEmpty()) {
                log.info("미처리 메트릭 증감 없음, 스킵");
                return;
            }

            Map<Long, Integer> likeDeltas = extractDeltas(unprocessedRecords, ProductMetricsDaily::getLikeDelta);
            Map<Long, Integer> viewDeltas = extractDeltas(unprocessedRecords, ProductMetricsDaily::getViewDelta);
            Map<Long, Integer> orderDeltas = extractDeltas(unprocessedRecords, ProductMetricsDaily::getOrderDelta);

            if (!likeDeltas.isEmpty()) {
                rankingFacade.incrementProductLikeRanking(likeDeltas);
            }
            if (!viewDeltas.isEmpty()) {
                rankingFacade.incrementProductViewRanking(viewDeltas);
            }
            if (!orderDeltas.isEmpty()) {
                rankingFacade.incrementProductOrderRanking(orderDeltas);
            }

            Map<Long, Double> compositeScores = calculateCompositeScores(likeDeltas, viewDeltas, orderDeltas);
            if (!compositeScores.isEmpty()) {
                rankingFacade.incrementProductAllRanking(compositeScores);
            }

            for (ProductMetricsDaily record : unprocessedRecords) {
                record.markAsProcessed();
            }
            productMetricsDailyRepository.saveAll(unprocessedRecords);

            log.info("미처리 메트릭 증감 처리 완료 - 레코드: {}, 좋아요: {}, 조회: {}, 주문: {}, 종합: {}",
                    unprocessedRecords.size(), likeDeltas.size(), viewDeltas.size(), orderDeltas.size(), compositeScores.size());

        } catch (Exception e) {
            log.error("미처리 메트릭 증감 처리 실패", e);
            throw new RuntimeException("메트릭 증감 처리 실패", e);
        }
    }

    private Map<Long, Integer> extractDeltas(List<ProductMetricsDaily> records,
                                              java.util.function.Function<ProductMetricsDaily, Integer> deltaExtractor) {
        return records.stream()
                .filter(record -> deltaExtractor.apply(record) != 0)
                .collect(Collectors.toMap(
                        ProductMetricsDaily::getProductId,
                        deltaExtractor,
                        Integer::sum
                ));
    }

    /**
     * 종합 점수 계산
     * - Score = (likeDelta * likeWeight) + (viewDelta * viewWeight) + (orderDelta * orderWeight);
     * @return 상품별 종합 점수
     */
    private Map<Long, Double> calculateCompositeScores(Map<Long, Integer> likeDeltas,
                                                        Map<Long, Integer> viewDeltas,
                                                        Map<Long, Integer> orderDeltas) {
        Set<Long> allProductIds = Stream.of(
                likeDeltas.keySet(),
                viewDeltas.keySet(),
                orderDeltas.keySet()
        ).flatMap(Set::stream).collect(Collectors.toSet());

        Map<Long, Double> compositeScores = new HashMap<>();

        for (Long productId : allProductIds) {
            int likeDelta = likeDeltas.getOrDefault(productId, 0);
            int viewDelta = viewDeltas.getOrDefault(productId, 0);
            int orderDelta = orderDeltas.getOrDefault(productId, 0);

            double compositeScore = (likeDelta * likeWeight) + (viewDelta * viewWeight) + (orderDelta * orderWeight);

            if (compositeScore != 0.0) {
                compositeScores.put(productId, compositeScore);
            }
        }

        return compositeScores;
    }

    /**
     * 오래된 일자별 데이터 정리 (매일 새벽 4시)
     * - 30일 이전 데이터 삭제
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldDailyMetrics() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            int deleted = productMetricsDailyRepository.deleteByMetricDateBefore(cutoffDate);
            log.info("오래된 일자별 데이터 정리 완료 - {} 건 삭제", deleted);
        } catch (Exception e) {
            log.error("일자별 데이터 정리 실패", e);
            throw new RuntimeException("일자별 데이터 정리 실패", e);
        }
    }
}
