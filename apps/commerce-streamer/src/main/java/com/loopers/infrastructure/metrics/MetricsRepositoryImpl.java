package com.loopers.infrastructure.metrics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.domain.metrics.repository.MetricsRepository;
import com.loopers.infrastructure.cache.ProductCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭 Repository 구현체
 * <p>
 * ProductMetricsRepository를 사용하여 실제 메트릭 업데이트를 수행합니다.
 * 존재하지 않는 상품의 경우 새로 생성하여 처리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MetricsRepositoryImpl implements MetricsRepository {

    private final ProductMetricsRepository productMetricsRepository;
    private final ProductCacheService productCacheService;

    @Override
    public void incrementView(Long productId, long occurredAtEpochMillis) {
        ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);

        Optional<ProductMetricsEntity> existingMetrics = productMetricsRepository.findById(productId);

        long newViewCount;
        if (existingMetrics.isPresent()) {
            ProductMetricsEntity metrics = existingMetrics.get();
            metrics.incrementView(eventTime);
            productMetricsRepository.save(metrics);
            newViewCount = metrics.getViewCount();
        } else {
            // 새로운 상품 메트릭 생성
            ProductMetricsEntity newMetrics = ProductMetricsEntity.create(productId);
            newMetrics.incrementView(eventTime);
            productMetricsRepository.save(newMetrics);
            newViewCount = newMetrics.getViewCount();
        }


        log.debug("조회수 증가 완료: productId={}, eventTime={}", productId, eventTime);
    }

    @Override
    public void applyLikeDelta(Long productId, int delta, long occurredAtEpochMillis) {
        ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);

        Optional<ProductMetricsEntity> existingMetrics = productMetricsRepository.findById(productId);

        if (existingMetrics.isPresent()) {
            ProductMetricsEntity metrics = existingMetrics.get();
            metrics.applyLikeDelta(delta, eventTime);
            productMetricsRepository.save(metrics);
        } else {
            // 새로운 상품 메트릭 생성 (좋아요가 음수가 되지 않도록 처리)
            if (delta > 0) {
                ProductMetricsEntity newMetrics = ProductMetricsEntity.create(productId);
                newMetrics.applyLikeDelta(delta, eventTime);
                productMetricsRepository.save(newMetrics);
            } else {
                log.debug("새로운 상품에 대한 좋아요 감소 무시: productId={}, delta={}", productId, delta);
                return; // 캐시 무효화 불필요
            }
        }

        // 좋아요는 캐시 무효화하지 않음 (실시간 반영 불필요)

        log.debug("좋아요 수 변경 완료: productId={}, delta={}, eventTime={}", productId, delta, eventTime);
    }

    @Override
    public void addSales(Long productId, int quantity, long occurredAtEpochMillis) {
        if (quantity <= 0) {
            log.debug("잘못된 판매량 무시: productId={}, quantity={}", productId, quantity);
            return;
        }

        ZonedDateTime eventTime = convertToZonedDateTime(occurredAtEpochMillis);

        Optional<ProductMetricsEntity> existingMetrics = productMetricsRepository.findById(productId);

        if (existingMetrics.isPresent()) {
            ProductMetricsEntity metrics = existingMetrics.get();
            metrics.addSales(quantity, eventTime);
            productMetricsRepository.save(metrics);
        } else {
            // 새로운 상품 메트릭 생성
            ProductMetricsEntity newMetrics = ProductMetricsEntity.create(productId);
            newMetrics.addSales(quantity, eventTime);
            productMetricsRepository.save(newMetrics);
        }

        // 캐시 무효화 (판매량 변경 - 인기 상품 순위 영향)
        productCacheService.onSalesCountChanged(productId);

        log.debug("판매량 증가 완료: productId={}, quantity={}, eventTime={}", productId, quantity, eventTime);
    }

    @Override
    public void handleStockDepleted(Long productId, Long brandId, Integer remainingStock, long occurredAtEpochMillis) {
        // 재고 소진 이벤트 처리
        // 메트릭 자체는 업데이트하지 않고 캐시만 처리

        // 상품 상세 캐시의 재고 정보만 갱신 (빠른 응답을 위해)
        int stockToUpdate = (remainingStock != null) ? remainingStock : 0;
        productCacheService.updateProductStock(productId, stockToUpdate);

        log.info("재고 소진 상세 캐시 갱신 완료: productId={}, brandId={}, remainingStock={}",
                productId, brandId, stockToUpdate);
    }

    /**
     * Epoch 밀리초를 ZonedDateTime으로 변환
     */
    private ZonedDateTime convertToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()
        );
    }
}
