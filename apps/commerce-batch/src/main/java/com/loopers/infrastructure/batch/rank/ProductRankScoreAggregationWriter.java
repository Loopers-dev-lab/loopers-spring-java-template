package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.rank.ProductRankScore;
import com.loopers.domain.rank.ProductRankScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ProductRankScore 집계를 위한 Writer.
 * <p>
 * Step 1 (집계 로직 계산 Step)에서 사용합니다.
 * Chunk 단위로 받은 ProductMetrics를 product_id별로 집계하여 점수를 계산하고,
 * ProductRankScore 임시 테이블에 저장합니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>Chunk 단위로 받은 ProductMetrics를 product_id별로 집계</li>
 *   <li>점수 계산 (가중치: 좋아요 0.3, 판매량 0.5, 조회수 0.2)</li>
 *   <li>ProductRankScore 테이블에 저장 (랭킹 번호 없이)</li>
 *   <li>같은 product_id가 여러 Chunk에 걸쳐 있을 경우 UPSERT 방식으로 누적</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankScoreAggregationWriter implements ItemWriter<ProductMetrics> {

    private final ProductRankScoreRepository productRankScoreRepository;

    /**
     * ProductMetrics Chunk를 집계하여 ProductRankScore 테이블에 저장합니다.
     * <p>
     * Chunk 단위로 받은 ProductMetrics를 product_id별로 집계하여 점수를 계산하고 저장합니다.
     * 같은 product_id가 여러 Chunk에 걸쳐 있을 경우, 기존 데이터를 조회하여 누적한 후 저장합니다.
     * </p>
     *
     * @param chunk 처리할 ProductMetrics Chunk
     * @throws Exception 처리 중 오류 발생 시
     */
    @Override
    public void write(Chunk<? extends ProductMetrics> chunk) throws Exception {
        List<? extends ProductMetrics> items = chunk.getItems();

        if (items.isEmpty()) {
            log.warn("ProductMetrics Chunk가 비어있습니다.");
            return;
        }

        log.debug("ProductRankScore Chunk 처리 시작: itemCount={}", items.size());

        // 같은 product_id를 가진 메트릭을 합산 (Chunk 내에서)
        Map<Long, AggregatedMetrics> chunkAggregatedMap = items.stream()
            .collect(Collectors.groupingBy(
                ProductMetrics::getProductId,
                Collectors.reducing(
                    new AggregatedMetrics(0L, 0L, 0L),
                    metrics -> new AggregatedMetrics(
                        metrics.getLikeCount(),
                        metrics.getSalesCount(),
                        metrics.getViewCount()
                    ),
                    (a, b) -> new AggregatedMetrics(
                        a.getLikeCount() + b.getLikeCount(),
                        a.getSalesCount() + b.getSalesCount(),
                        a.getViewCount() + b.getViewCount()
                    )
                )
            ));

        // Chunk 내 모든 productId를 한 번에 조회
        Set<Long> productIds = chunkAggregatedMap.keySet();
        Map<Long, ProductRankScore> existingScores = productRankScoreRepository
            .findAllByProductIdIn(productIds)
            .stream()
            .collect(Collectors.toMap(ProductRankScore::getProductId, Function.identity()));

        // 기존 데이터와 누적하여 ProductRankScore 생성
        List<ProductRankScore> scores = chunkAggregatedMap.entrySet().stream()
            .map(entry -> {
                Long productId = entry.getKey();
                AggregatedMetrics chunkAggregated = entry.getValue();
                
                // 기존 데이터 조회 (일괄 조회 결과에서)
                ProductRankScore existing = existingScores.get(productId);
                
                // 기존 데이터와 누적
                Long totalLikeCount = chunkAggregated.getLikeCount();
                Long totalSalesCount = chunkAggregated.getSalesCount();
                Long totalViewCount = chunkAggregated.getViewCount();
                
                if (existing != null) {
                    totalLikeCount += existing.getLikeCount();
                    totalSalesCount += existing.getSalesCount();
                    totalViewCount += existing.getViewCount();
                }
                
                // 점수 계산 (가중치: 좋아요 0.3, 판매량 0.5, 조회수 0.2)
                double score = calculateScore(totalLikeCount, totalSalesCount, totalViewCount);
                
                return new ProductRankScore(
                    productId,
                    totalLikeCount,
                    totalSalesCount,
                    totalViewCount,
                    score
                );
            })
            .collect(Collectors.toList());

        // 저장 (기존 데이터가 있으면 덮어쓰기)
        productRankScoreRepository.saveAll(scores);

        log.debug("ProductRankScore 저장 완료: count={}", scores.size());
    }

    /**
     * 종합 점수를 계산합니다.
     * <p>
     * 가중치:
     * <ul>
     *   <li>좋아요: 0.3</li>
     *   <li>판매량: 0.5</li>
     *   <li>조회수: 0.2</li>
     * </ul>
     * </p>
     *
     * @param likeCount 좋아요 수
     * @param salesCount 판매량
     * @param viewCount 조회 수
     * @return 종합 점수
     */
    private double calculateScore(Long likeCount, Long salesCount, Long viewCount) {
        return likeCount * 0.3 + salesCount * 0.5 + viewCount * 0.2;
    }

    /**
     * 집계된 메트릭을 담는 내부 클래스.
     */
    private static class AggregatedMetrics {
        private final Long likeCount;
        private final Long salesCount;
        private final Long viewCount;

        public AggregatedMetrics(Long likeCount, Long salesCount, Long viewCount) {
            this.likeCount = likeCount;
            this.salesCount = salesCount;
            this.viewCount = viewCount;
        }

        public Long getLikeCount() {
            return likeCount;
        }

        public Long getSalesCount() {
            return salesCount;
        }

        public Long getViewCount() {
            return viewCount;
        }
    }
}

