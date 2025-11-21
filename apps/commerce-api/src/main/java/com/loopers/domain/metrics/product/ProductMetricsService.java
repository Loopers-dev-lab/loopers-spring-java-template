package com.loopers.domain.metrics.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductMetricsService {
    private final ProductMetricsRepository productMetricsRepository;

    public ProductMetrics getMetricsByProductId(Long productId) {
        return productMetricsRepository.findByProductId(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다."));
    }

    public Map<Long, ProductMetrics> getMetricsMapByProductIds(Collection<Long> productIds) {
        return productMetricsRepository.findByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductMetrics::getProductId, metrics -> metrics));
    }

    // pageable like_count 요건에 따라 정렬된 상위 N개 상품 메트릭 조회
    public Page<ProductMetrics> getMetrics(Pageable pageable) {
        // 현재는 like_count, desc만 가지므로, 예외처리 필요
        String sortString = pageable.getSort().toString();
        if (!sortString.equals("likeCount: DESC")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 정렬 방식입니다.");
        }
        return productMetricsRepository.findAll(pageable);
    }
}
