package com.loopers.application.ranking.batch;

import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class ProductMetricsReader {//} extends JpaPagingItemReader<ProductMetrics> {
    private final ProductMetricsRepository productMetricsRepository;


    public JpaPagingItemReader<ProductMetrics> createReader(ZonedDateTime startDate, ZonedDateTime endDate) {

        JpaPagingItemReader<ProductMetrics> reader = new JpaPagingItemReader<>();
        reader.setQueryString("SELECT pm FROM ProductMetrics pm WHERE pm.metricsDate >= :startDate AND pm.metricsDate < :endDate ORDER BY pm.metricsDate ASC");
        reader.setParameterValues(
                java.util.Map.of(
                        "startDate", startDate,
                        "endDate", endDate
                )
        );
        reader.setPageSize(100);
        reader.setEntityManagerFactory(
                productMetricsRepository.getEntityManager().getEntityManagerFactory()
        );
        return reader;
    }
}
