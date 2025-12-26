package com.loopers.domain.metrics;

import java.util.Optional;

/**
 * 상품 메트릭 Repository 인터페이스
 * <p>
 * Domain 계층의 순수한 Repository 인터페이스입니다.
 * Infrastructure 계층에서 JPA로 구현됩니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface ProductMetricsRepository {
    
    /**
     * 메트릭 저장
     */
    ProductMetricsEntity save(ProductMetricsEntity metrics);

    /**
     * 상품 ID로 메트릭 조회
     */
    Optional<ProductMetricsEntity> findById(Long productId);
}
