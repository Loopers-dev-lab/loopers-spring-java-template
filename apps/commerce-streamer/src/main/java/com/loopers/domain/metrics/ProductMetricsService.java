package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 메트릭 Domain Service
 * <p>
 * 상품 메트릭 관련 비즈니스 로직을 담당하는 Domain 계층 서비스입니다.
 * 트랜잭션 경계를 관리하고 Repository를 통해 데이터를 조작합니다.
 * <p>
 * 일간 집계를 위해 이벤트 시간에서 날짜를 추출하여 메트릭을 관리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 26.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 조회수 증가
     *
     * @param productId 상품 ID
     * @param eventTime 이벤트 발생 시간
     */
    @Transactional
    public void incrementView(Long productId, ZonedDateTime eventTime) {
        LocalDate metricDate = eventTime.toLocalDate();
        ProductMetricsEntity metrics = getOrCreateMetrics(productId, metricDate);
        metrics.incrementView(eventTime);
        productMetricsRepository.save(metrics);
        log.debug("조회수 증가 완료: productId={}, date={}", productId, metricDate);
    }

    /**
     * 좋아요 수 변경
     *
     * @param productId 상품 ID
     * @param delta     변경량 (양수: 증가, 음수: 감소)
     * @param eventTime 이벤트 발생 시간
     * @return true: 변경됨, false: 변경 안 됨 (새 상품에 대한 좋아요 감소)
     */
    @Transactional
    public boolean applyLikeDelta(Long productId, int delta, ZonedDateTime eventTime) {
        LocalDate metricDate = eventTime.toLocalDate();
        Optional<ProductMetricsEntity> existing = productMetricsRepository
                .findByProductIdAndMetricDate(productId, metricDate);

        if (existing.isPresent()) {
            ProductMetricsEntity metrics = existing.get();
            metrics.applyLikeDelta(delta, eventTime);
            productMetricsRepository.save(metrics);
            log.debug("좋아요 수 변경 완료: productId={}, delta={}, date={}", productId, delta, metricDate);
            return true;
        } else if (delta > 0) {
            // 새로운 상품에 대한 좋아요 추가만 허용
            ProductMetricsEntity newMetrics = ProductMetricsEntity.create(productId, metricDate);
            newMetrics.applyLikeDelta(delta, eventTime);
            productMetricsRepository.save(newMetrics);
            log.debug("새 상품 좋아요 추가 완료: productId={}, delta={}, date={}", productId, delta, metricDate);
            return true;
        } else {
            log.debug("새로운 상품에 대한 좋아요 감소 무시: productId={}, delta={}", productId, delta);
            return false;
        }
    }

    /**
     * 판매량 증가
     *
     * @param productId 상품 ID
     * @param quantity  판매 수량
     * @param eventTime 이벤트 발생 시간
     * @return true: 증가됨, false: 증가 안 됨 (잘못된 수량)
     */
    @Transactional
    public boolean addSales(Long productId, int quantity, ZonedDateTime eventTime) {
        if (quantity <= 0) {
            log.debug("잘못된 판매량 무시: productId={}, quantity={}", productId, quantity);
            return false;
        }

        LocalDate metricDate = eventTime.toLocalDate();
        ProductMetricsEntity metrics = getOrCreateMetrics(productId, metricDate);
        metrics.addSales(quantity, eventTime);
        productMetricsRepository.save(metrics);
        log.debug("판매량 증가 완료: productId={}, quantity={}, date={}", productId, quantity, metricDate);
        return true;
    }

    /**
     * 상품 메트릭 조회 또는 생성
     *
     * @param productId  상품 ID
     * @param metricDate 메트릭 날짜
     * @return 메트릭 엔티티
     */
    private ProductMetricsEntity getOrCreateMetrics(Long productId, LocalDate metricDate) {
        return productMetricsRepository
                .findByProductIdAndMetricDate(productId, metricDate)
                .orElseGet(() -> ProductMetricsEntity.create(productId, metricDate));
    }
}
