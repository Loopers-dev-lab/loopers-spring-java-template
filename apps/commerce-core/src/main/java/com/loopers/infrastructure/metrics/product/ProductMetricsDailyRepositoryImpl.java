package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetricsDaily;
import com.loopers.domain.metrics.product.ProductMetricsDailyAggregated;
import com.loopers.domain.metrics.product.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMetricsDailyRepositoryImpl implements ProductMetricsDailyRepository {
    
    private final ProductMetricsDailyJpaRepository jpaRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ProductMetricsDaily> findByProductIdAndDate(Long productId, LocalDate date) {
        return jpaRepository.findByProductIdAndDate(productId, date);
    }
    
    @Override
    @Transactional
    public Optional<ProductMetricsDaily> findByProductIdAndDateForUpdate(Long productId, LocalDate date) {
        return jpaRepository.findByProductIdAndDateForUpdate(productId, date);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductMetricsDaily> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByDateBetween(startDate, endDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductMetricsDailyAggregated> findAggregatedByDateBetween(
        LocalDate startDate, 
        LocalDate endDate
    ) {
        List<Object[]> results = jpaRepository.findAggregatedByDateBetween(startDate, endDate);
        return results.stream()
            .map(row -> new ProductMetricsDailyAggregated(
                ((Number) row[0]).longValue(),      // productId
                ((Number) row[1]).longValue(),      // totalLikeCount
                ((Number) row[2]).longValue(),      // totalViewCount
                ((Number) row[3]).longValue()       // totalSoldCount
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Page<ProductMetricsDailyAggregated> findAggregatedByDateBetweenPaged(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Object[]> results = jpaRepository.findAggregatedByDateBetweenPaged(
                startDate, endDate, pageable
        );

        return results.map(row -> new ProductMetricsDailyAggregated(
                ((Number) row[0]).longValue(),      // productId
                ((Number) row[1]).longValue(),      // totalLikeCount
                ((Number) row[2]).longValue(),      // totalViewCount
                ((Number) row[3]).longValue()       // totalSoldCount
        ));
    }

    @Override
    @Transactional
    public ProductMetricsDaily save(ProductMetricsDaily daily) {
        return jpaRepository.save(daily);
    }
    
    @Override
    @Transactional
    public List<ProductMetricsDaily> saveAll(Collection<ProductMetricsDaily> dailies) {
        return jpaRepository.saveAll(dailies);
    }
}


