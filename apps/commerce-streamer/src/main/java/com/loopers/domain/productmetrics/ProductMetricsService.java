package com.loopers.domain.productmetrics;

import com.loopers.infrastructure.productmetrics.ProductMetricsJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsJpaRepository productMetricsRepository;

    @Transactional
    public void increaseSalesCount(Long productId, int quantity, BigDecimal amount, LocalDate metricDate) {
        if (quantity <= 0) {
            log.warn("판매 수량이 0 이하일 수 없습니다. 수량:{}, 상품ID:{}, 일자:{}", quantity, productId, metricDate);
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0){
            log.warn("판매 금액이 0 이하일 수 없습니다. 금액:{}, 상품ID:{}", amount, productId);
            return;
        }
        productMetricsRepository.upsertSalesCount(productId, quantity, amount, metricDate);
    }

    @Transactional
    public void increaseLikeCount(Long productId, LocalDate metricDate) {
        productMetricsRepository.upsertLikeCount(productId, metricDate);
    }

    @Transactional
    public void decreaseLikeCount(Long productId, LocalDate metricDate) {
        productMetricsRepository.upsertUnlikeCount(productId, metricDate);
    }

    @Transactional
    public void increaseViewCount(Long productId, LocalDate metricDate) {
        productMetricsRepository.upsertViewCount(productId, metricDate);
    }

    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsRepository.findByProductId(productId);
    }
}
