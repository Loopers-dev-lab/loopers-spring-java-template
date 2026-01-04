package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ProductMetrics를 처리하는 Spring Batch ItemWriter.
 * <p>
 * 현재는 로깅만 수행하지만, 향후 Materialized View에 저장하는 로직을 추가할 수 있습니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>Chunk 단위로 데이터를 처리하여 대량 데이터 처리 성능 최적화</li>
 *   <li>향후 주간/월간 랭킹을 위한 Materialized View 저장 로직 추가 예정</li>
 *   <li>트랜잭션 단위는 Chunk 단위로 관리</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
public class ProductMetricsItemWriter implements ItemWriter<ProductMetrics> {

    /**
     * ProductMetrics Chunk를 처리합니다.
     * <p>
     * 현재는 로깅만 수행하며, 향후 Materialized View에 저장하는 로직을 추가할 예정입니다.
     * </p>
     *
     * @param chunk 처리할 ProductMetrics Chunk
     * @throws Exception 처리 중 오류 발생 시
     */
    @Override
    public void write(Chunk<? extends ProductMetrics> chunk) throws Exception {
        List<? extends ProductMetrics> items = chunk.getItems();
        
        log.info("ProductMetrics Chunk 처리 시작: itemCount={}", items.size());
        
        // 현재는 로깅만 수행
        // 향후 주간/월간 랭킹을 위한 Materialized View 저장 로직 추가 예정
        for (ProductMetrics item : items) {
            log.debug("ProductMetrics 처리: productId={}, likeCount={}, salesCount={}, viewCount={}, updatedAt={}",
                item.getProductId(), item.getLikeCount(), item.getSalesCount(), 
                item.getViewCount(), item.getUpdatedAt());
        }
        
        log.info("ProductMetrics Chunk 처리 완료: itemCount={}", items.size());
    }
}

