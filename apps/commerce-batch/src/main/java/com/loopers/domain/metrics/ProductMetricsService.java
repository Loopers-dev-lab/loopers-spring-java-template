package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    public List<ProductMetrics> findByMetricsDate(LocalDate metricsDate) {
        return productMetricsRepository.findByMetricsDate(metricsDate);
    }
}

