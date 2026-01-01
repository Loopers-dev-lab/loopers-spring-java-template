package com.loopers.application.batch.product;

import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Objects;

@RequiredArgsConstructor
public class MonthlyProductMetricBatchReader implements ItemStreamReader<ProductMetricAggregation> {

    private final DailyProductMetricRepository dailyProductMetricRepository;
    private Iterator<ProductMetricAggregation> iterator;

    @Override
    public void open(@NonNull ExecutionContext executionContext) {
        LocalDate startDate = LocalDate.parse(executionContext.getString("startDate"));
        LocalDate endDate = LocalDate.parse(executionContext.getString("endDate"));
        long partitionOffset = executionContext.getLong("partitionOffset");
        long partitionLimit = executionContext.getLong("partitionLimit");

        this.iterator = dailyProductMetricRepository.findAggregatedBy(startDate, endDate, partitionOffset, partitionLimit)
                .iterator();
    }

    @Override
    public ProductMetricAggregation read() {
        if (Objects.isNull(iterator) || !iterator.hasNext()) {
            return null;
        }

        return iterator.next();
    }
}
