package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ProductMetrics를 처리하는 Spring Batch ItemProcessor.
 * <p>
 * 현재는 데이터를 그대로 전달하지만, 향후 집계 로직을 추가할 수 있습니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>Reader와 Writer 사이의 변환/필터링 로직을 위한 확장 포인트 제공</li>
 *   <li>향후 주간/월간 집계를 위한 데이터 변환 로직 추가 가능</li>
 *   <li>비즈니스 로직 검증 및 필터링 수행 가능</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
public class ProductMetricsItemProcessor implements ItemProcessor<ProductMetrics, ProductMetrics> {

    /**
     * ProductMetrics를 처리합니다.
     * <p>
     * 현재는 데이터를 그대로 전달하지만, 필요시 변환/필터링 로직을 추가할 수 있습니다.
     * </p>
     *
     * @param item 처리할 ProductMetrics
     * @return 처리된 ProductMetrics (null 반환 시 해당 항목은 Writer로 전달되지 않음)
     */
    @Override
    public ProductMetrics process(ProductMetrics item) throws Exception {
        // 현재는 데이터를 그대로 전달
        // 향후 집계 로직이나 데이터 변환이 필요하면 여기에 추가
        return item;
    }
}

