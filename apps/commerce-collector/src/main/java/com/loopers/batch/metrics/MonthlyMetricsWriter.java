package com.loopers.batch.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * 월간 집계 결과를 MV 테이블에 저장하는 Writer
 */
@Slf4j
@RequiredArgsConstructor
public class MonthlyMetricsWriter implements ItemWriter<ProductMetricsMonthly> {

    private final ProductMetricsMonthlyRepository monthlyRepository;

    @Override
    public void write(Chunk<? extends ProductMetricsMonthly> chunk) {
        List<? extends ProductMetricsMonthly> items = chunk.getItems();

        // Bulk Insert/Update (UPSERT)
        monthlyRepository.saveAll((List<ProductMetricsMonthly>) items);

        log.info("월간 집계 저장 완료: {} 건", items.size());
    }
}
