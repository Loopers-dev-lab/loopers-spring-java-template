package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ProductMetricsRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 ProductMetrics 엔티티의
 * 영속성 작업을 처리합니다.
 * </p>
 * <p>
 * <b>배치 전용 구현:</b>
 * <ul>
 *   <li>Spring Batch에서 날짜 기반 조회에 최적화</li>
 *   <li>대량 데이터 처리를 위한 페이징 조회 지원</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return productMetricsJpaRepository.save(productMetrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ProductMetrics> findByUpdatedAtBetween(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Pageable pageable
    ) {
        return productMetricsJpaRepository.findByUpdatedAtBetween(startDateTime, endDateTime, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.springframework.data.repository.PagingAndSortingRepository<ProductMetrics, Long> getJpaRepository() {
        return productMetricsJpaRepository;
    }
}

