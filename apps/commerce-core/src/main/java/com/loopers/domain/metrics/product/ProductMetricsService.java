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
    public void incrementViewCount(Long productId) {
        ProductMetrics productMetrics = productMetricsRepository
                .findByProductIdForUpdate(productId)
                .orElseGet(() -> {
                    // ProductMetrics가 없으면 생성 필요
                    // 하지만 productId만으로는 brandId를 알 수 없으므로, 
                    // 실제로는 Product 서비스를 통해 brandId를 가져와야 함
                    throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다.");
                });
        productMetrics.incrementViewCount();
        productMetricsRepository.save(productMetrics);
        log.debug("Incremented view count for product: productId={}", productId);
    }

    @Transactional
    public void incrementSoldCount(Long productId, Long quantity) {
        ProductMetrics productMetrics = productMetricsRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다."));
        productMetrics.incrementSoldCount(quantity);
        productMetricsRepository.save(productMetrics);
        log.debug("Incremented sold count for product: productId={}, quantity={}", productId, quantity);
    }

    @Transactional
    public List<ProductMetrics> saveAll(Collection<ProductMetrics> list) {
        return productMetricsRepository.saveAll(list);
    }

    /**
     * @deprecated Kafka 이벤트로 전환됨
     * 좋아요 이벤트는 이제 Kafka를 통해 비동기로 처리됩니다.
     * - Producer: LikeProductEventPublisherImpl이 Outbox에 저장
     * - Consumer: commerce-streamer의 CatalogEventListener가 Kafka에서 수신하여 처리
     * 
     * 이 메서드는 하위 호환성을 위해 유지하되, 실제로는 호출되지 않습니다.
     */
    @Deprecated
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLikeProductEvent(LikeProductEvent event) {
        log.warn("handleLikeProductEvent is deprecated. LikeProductEvent is now processed via Kafka.");
        log.info("LikeProductEvent 수신. in {}.\nevent: {}", this.getClass().getSimpleName(), event);
        if (event == null) {
            log.warn("null event 수신. in {}. 무시합니다.", this.getClass().getSimpleName());
            return;
        }
        // Kafka 이벤트로 전환되었으므로 여기서는 처리하지 않음
        // 실제 처리는 commerce-streamer의 CatalogEventListener에서 수행
    }
}
