package com.loopers.domain.productmetrics;

import com.loopers.infrastructure.productmetrics.ProductMetricsJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetricsService {

    private final ProductMetricsJpaRepository productMetricsRepository;

    @Transactional
    public void increaseSalesCount(Long productId, int quantity) {
        if (quantity <= 0) {
            log.warn("판매 수량이 0 이하일 수 없습니다. 수량:{}, 상품ID:{}", quantity, productId);
            return;
        }
        productMetricsRepository.upsertSalesCount(productId, quantity);
    }

    @Transactional
    public void increaseLikeCount(Long productId) {
        productMetricsRepository.upsertLikeCount(productId);
    }

    @Transactional
    public void decreaseLikeCount(Long productId) {
        productMetricsRepository.upsertUnlikeCount(productId);
    }

    @Transactional
    public void increaseViewCount(Long productId) {
        productMetricsRepository.upsertViewCount(productId);
    }
}
