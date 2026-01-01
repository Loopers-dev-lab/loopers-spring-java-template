package com.loopers.application.ranking;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProductMetricsItemReader implements ItemReader<ProductMetrics>, ItemStream {

    private static final String CURRENT_INDEX_KEY = "current.index";

    private final ProductMetricsService productMetricsService;
    private StepExecution stepExecution;
    private List<ProductMetrics> productMetricsList;
    private int currentIndex = 0;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public ProductMetrics read() {
        if (productMetricsList == null || currentIndex >= productMetricsList.size()) {
            return null;
        }
        return productMetricsList.get(currentIndex++);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(CURRENT_INDEX_KEY)) {
            currentIndex = executionContext.getInt(CURRENT_INDEX_KEY);
        } else {
            currentIndex = 0;
        }

        LocalDate targetDate = JobParameterUtils.getTargetDate(stepExecution);
        log.info("ProductMetrics 조회 시작: targetDate={}", targetDate);
        productMetricsList = productMetricsService.findByMetricsDate(targetDate);
        log.info("ProductMetrics 조회 완료: size={}", productMetricsList.size());
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CURRENT_INDEX_KEY, currentIndex);
    }

    @Override
    public void close() throws ItemStreamException {
        productMetricsList = null;
        currentIndex = 0;
    }
}

