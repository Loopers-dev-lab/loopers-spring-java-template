package com.loopers.domain.metrics.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductMetricsService {
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
        productMetricsRepository.findByProductIdForUpdate(productId)
                .ifPresentOrElse(productMetrics -> {
                            productMetrics.incrementLikeCount();
                            productMetricsRepository.save(productMetrics);
                        },
                        () -> {
                            throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다.");
                        });
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        productMetricsRepository.findByProductIdForUpdate(productId)
                .ifPresentOrElse(productMetrics -> {
                            productMetrics.decrementLikeCount();
                            productMetricsRepository.save(productMetrics);
                        },
                        () -> {
                            throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다.");
                        });
    }

    @Transactional
    public List<ProductMetrics> saveAll(Collection<ProductMetrics> list) {
        return productMetricsRepository.saveAll(list);
    }
}
