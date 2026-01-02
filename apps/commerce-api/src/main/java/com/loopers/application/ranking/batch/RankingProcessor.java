package com.loopers.application.ranking.batch;

import com.loopers.domain.metrics.product.ProductMetricsDailyAggregated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RankingProcessor implements ItemProcessor<ProductMetricsDailyAggregated, ProductMetricsDailyAggregated> {

    private static final ThreadLocal<PriorityQueue<ProductMetricsDailyAggregated>> top100Queue =
            ThreadLocal.withInitial(() -> new PriorityQueue<>(
                    100,
                    Comparator.comparing(ProductMetricsDailyAggregated::calculateScore)
            ));

    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        top100Queue.remove();
        top100Queue.set(new PriorityQueue<>(
                100,
                Comparator.comparing(ProductMetricsDailyAggregated::calculateScore)
        ));
        log.info("[RankingProcessor] beforeStep: PriorityQueue 초기화 완료, stepName={}, thread={}",
                stepExecution.getStepName(), Thread.currentThread().getName());
    }

    @Override
    public ProductMetricsDailyAggregated process(ProductMetricsDailyAggregated item) throws Exception {
        if (item == null) {
            log.warn("[RankingProcessor] process: null 아이템 수신");
            return null;
        }

        log.info("[RankingProcessor] process: 아이템 처리 중 - productId={}, score={}, likeCount={}, viewCount={}, soldCount={}",
                item.getProductId(), item.calculateScore(), item.getTotalLikeCount(), item.getTotalViewCount(), item.getTotalSoldCount());

        PriorityQueue<ProductMetricsDailyAggregated> queue = top100Queue.get();
        if (queue == null) {
            log.error("[RankingProcessor] process: PriorityQueue가 null입니다! 초기화 중...");
            queue = new PriorityQueue<>(100, Comparator.comparing(ProductMetricsDailyAggregated::calculateScore));
            top100Queue.set(queue);
        }

        synchronized (queue) {
            if (queue.size() < 100) {
                queue.offer(item);
                log.info("[RankingProcessor] process: 큐에 아이템 추가 - productId={}, score={}, queueSize={}",
                        item.getProductId(), item.calculateScore(), queue.size());
            } else {
                ProductMetricsDailyAggregated min = queue.peek();
                if (min != null && item.calculateScore() > min.calculateScore()) {
                    ProductMetricsDailyAggregated removed = queue.poll();
                    queue.offer(item);
                    log.info("[RankingProcessor] process: 큐에서 아이템 교체 - 제거된 productId={} (score={}), 추가된 productId={} (score={}), queueSize={}",
                            removed.getProductId(), removed.calculateScore(), item.getProductId(), item.calculateScore(), queue.size());
                } else {
                    log.debug("[RankingProcessor] process: 아이템 거부됨 - productId={}, score={}, minScore={}",
                            item.getProductId(), item.calculateScore(), min != null ? min.calculateScore() : "null");
                }
            }
        }

        return null;
    }

    @AfterChunk
    public void afterChunk(ChunkContext chunkContext) {
        PriorityQueue<ProductMetricsDailyAggregated> queue = top100Queue.get();
        if (queue == null) {
            log.warn("[RankingProcessor] afterChunk: PriorityQueue가 null입니다!");
        } else if (queue.isEmpty()) {
            log.warn("[RankingProcessor] afterChunk: PriorityQueue가 비어있습니다!");
        } else {
            log.info("[RankingProcessor] afterChunk: 청크 처리 완료 - queue size={}, thread={}",
                    queue.size(), Thread.currentThread().getName());
            ProductMetricsDailyAggregated min = queue.peek();
            List<ProductMetricsDailyAggregated> items = new ArrayList<>(queue);
            items.sort(Comparator.comparingDouble(ProductMetricsDailyAggregated::calculateScore).reversed());
            if (!items.isEmpty()) {
                log.info("[RankingProcessor] afterChunk: 큐 통계 - minScore={}, maxScore={}, top3ProductIds={}",
                        min != null ? min.calculateScore() : "null",
                        items.get(0).calculateScore(),
                        items.stream().limit(3).map(ProductMetricsDailyAggregated::getProductId).toList());
            }
        }
    }

    public static PriorityQueue<ProductMetricsDailyAggregated> getTop100Queue() {
        PriorityQueue<ProductMetricsDailyAggregated> queue = top100Queue.get();
        if (queue == null) {
            log.warn("[RankingProcessor] getTop100Queue: PriorityQueue가 null입니다!");
        } else {
            log.info("[RankingProcessor] getTop100Queue: 큐 반환 - size={}, thread={}",
                    queue.size(), Thread.currentThread().getName());
        }
        return queue;
    }

    public static void clearTop100Queue() {
        top100Queue.remove();
    }
}
