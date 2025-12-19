package com.loopers.domain.metrics.product;

import com.loopers.domain.like.product.LikeProductEvent;
import com.loopers.domain.like.product.LikeProductEventHandler;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductMetricsService implements LikeProductEventHandler {
    private final ProductMetricsRepository productMetricsRepository;

    @Transactional(readOnly = true)
    public ProductMetrics getMetricsByProductId(Long productId) {
        return productMetricsRepository.findByProductId(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductMetrics> getMetricsMapByProductIds(Collection<Long> productIds) {
        return productMetricsRepository.findByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductMetrics::getProductId, metrics -> metrics));
    }

    public Page<ProductMetrics> getMetrics(List<Long> brandIds, Pageable pageable) {
        String sortString = pageable.getSort().toString();
        if (!sortString.equals("likeCount: DESC")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 정렬 방식입니다.");
        }
        return productMetricsRepository.findAll(brandIds, pageable);
    }

    public ProductMetrics save(ProductMetrics productMetrics) {
        return productMetricsRepository.save(productMetrics);
    }

    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics productMetrics = productMetricsRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다."));
        productMetrics.incrementLikeCount();
        productMetricsRepository.save(productMetrics);
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        ProductMetrics productMetrics = productMetricsRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다."));
        productMetrics.decrementLikeCount();
        productMetricsRepository.save(productMetrics);
    }

    @Transactional
    public List<ProductMetrics> saveAll(Collection<ProductMetrics> list) {
        return productMetricsRepository.saveAll(list);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeProductEvent(LikeProductEvent event) {
        log.info("LikeProductEvent 수신. in {}.\nevent: {}", this.getClass().getSimpleName(), event);
        if (event == null) {
            log.warn("null event 수신. in {}. 무시합니다.", this.getClass().getSimpleName());
            return;
        }
        if (event.getLiked()) {
            incrementLikeCount(event.getProductId());
        } else {
            decrementLikeCount(event.getProductId());
        }
    }
}
