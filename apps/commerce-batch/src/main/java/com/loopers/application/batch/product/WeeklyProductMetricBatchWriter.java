package com.loopers.application.batch.product;

import com.loopers.core.domain.common.vo.YearMonthWeek;
import com.loopers.core.domain.product.WeeklyProductMetric;
import com.loopers.core.domain.product.repository.WeeklyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;
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
public class WeeklyProductMetricBatchWriter implements ItemStreamWriter<ProductMetricAggregation> {

    private final WeeklyProductMetricRepository repository;
    private YearMonthWeek yearMonthWeek;

    @Override
    public void open(@NonNull ExecutionContext executionContext) {
        LocalDate startDate = LocalDate.parse(executionContext.getString("startDate"));
        this.yearMonthWeek = YearMonthWeek.from(startDate);
    }

    @Override
    public void write(@NonNull Chunk<? extends ProductMetricAggregation> chunk) {
        List<WeeklyProductMetric> weeklyMetrics = chunk.getItems().stream()
                .map(aggregation -> aggregation.to(yearMonthWeek))
                .toList();

        repository.bulkUpsert(weeklyMetrics);
    }
}
