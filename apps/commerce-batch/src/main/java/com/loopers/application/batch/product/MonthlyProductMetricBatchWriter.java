package com.loopers.application.batch.product;

import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.repository.MonthlyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;
import com.loopers.core.domain.product.vo.YearMonth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyProductMetricBatchWriter implements ItemStreamWriter<ProductMetricAggregation> {

    private final MonthlyProductMetricRepository repository;
    private YearMonth yearMonth;

    @Override
    public void open(@NonNull ExecutionContext executionContext) {
        LocalDate startDate = LocalDate.parse(executionContext.getString("startDate"));
        this.yearMonth = YearMonth.from(startDate);
    }

    @Override
    public void write(@NonNull Chunk<? extends ProductMetricAggregation> chunk) {
        List<MonthlyProductMetric> monthlyMetrics = chunk.getItems().stream()
                .map(aggregation -> aggregation.to(yearMonth))
                .toList();

        repository.bulkUpsert(monthlyMetrics);
    }
}
