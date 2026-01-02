package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 상품 메트릭 엔티티 (일간 집계)
 * <p>
 * 상품별 일간 메트릭을 저장합니다.
 * 복합키(product_id + metric_date)를 사용하여 일간 집계를 구분합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Entity
@Getter
@Table(name = "product_metrics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsEntity {

    @EmbeddedId
    private ProductMetricsId id;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0L;

    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0L;

    @Column(name = "order_count", nullable = false)
    private long orderCount = 0L;

    @Column(name = "last_event_at")
    private ZonedDateTime lastEventAt;

    private ProductMetricsEntity(Long productId, LocalDate metricDate) {
        Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
        Objects.requireNonNull(metricDate, "메트릭 날짜는 필수입니다.");
        this.id = ProductMetricsId.of(productId, metricDate);
    }

    /**
     * 상품 메트릭 엔티티 생성
     *
     * @param productId  상품 ID
     * @param metricDate 메트릭 날짜
     * @return ProductMetricsEntity 인스턴스
     */
    public static ProductMetricsEntity create(Long productId, LocalDate metricDate) {
        return new ProductMetricsEntity(productId, metricDate);
    }

    // === 편의 메서드 ===

    /**
     * 상품 ID 조회
     */
    public Long getProductId() {
        return id.getProductId();
    }

    /**
     * 메트릭 날짜 조회
     */
    public LocalDate getMetricDate() {
        return id.getMetricDate();
    }

    // === 비즈니스 메서드 ===

    /**
     * 조회수 증가
     *
     * @param eventTime 이벤트 발생 시간
     */
    public void incrementView(ZonedDateTime eventTime) {
        this.viewCount += 1;
        this.lastEventAt = eventTime;
    }

    /**
     * 좋아요 수 변경
     * <p>
     * 좋아요 수는 0 미만으로 내려가지 않도록 보장합니다.
     *
     * @param delta     변경량 (양수: 증가, 음수: 감소)
     * @param eventTime 이벤트 발생 시간
     */
    public void applyLikeDelta(int delta, ZonedDateTime eventTime) {
        long next = this.likeCount + delta;
        this.likeCount = Math.max(0, next);
        this.lastEventAt = eventTime;
    }

    /**
     * 판매량 증가
     *
     * @param quantity  판매 수량
     * @param eventTime 이벤트 발생 시간
     */
    public void addSales(int quantity, ZonedDateTime eventTime) {
        if (quantity <= 0) {
            return;
        }
        this.salesCount += quantity;
        this.orderCount += 1;
        this.lastEventAt = eventTime;
    }
}
