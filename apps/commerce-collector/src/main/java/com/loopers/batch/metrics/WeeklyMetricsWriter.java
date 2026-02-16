package com.loopers.batch.metrics;

import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsWeeklyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class WeeklyMetricsWriter implements ItemWriter<ProductMetricsWeekly> {
    private final ProductMetricsWeeklyRepository weeklyRepository;

    @Override
    public void write(Chunk<? extends ProductMetricsWeekly> chunk) {
        List<ProductMetricsWeekly> items = (List<ProductMetricsWeekly>) chunk.getItems();

        weeklyRepository.saveAll(items);

        log.info("주간 집계 저장 완료: {} 건", items.size());
    }
}
