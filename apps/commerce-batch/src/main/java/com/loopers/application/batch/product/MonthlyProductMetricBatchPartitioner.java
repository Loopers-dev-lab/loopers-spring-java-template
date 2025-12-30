package com.loopers.application.batch.product;

import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class MonthlyProductMetricBatchPartitioner implements Partitioner {

    private final DailyProductMetricRepository dailyProductMetricRepository;

    @Value("#{jobParameters['startDate']}")
    private String startDateParam;

    @Value("#{jobParameters['endDate']}")
    private String endDateParam;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        LocalDate startDate = LocalDate.parse(startDateParam);
        LocalDate endDate = LocalDate.parse(endDateParam);
        Long totalCount = dailyProductMetricRepository.countDistinctProductIdsBy(startDate, endDate);

        if (totalCount == 0) {
            return Collections.emptyMap();
        }

        long targetSize = (totalCount / gridSize) + 1;
        Map<String, ExecutionContext> partitions = new HashMap<>();

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();

            context.putLong("partitionOffset", i * targetSize);
            context.putLong("partitionLimit", targetSize);
            context.putString("startDate", startDateParam);
            context.putString("endDate", endDateParam);

            partitions.put("partition" + i, context);
        }

        return partitions;
    }
}
