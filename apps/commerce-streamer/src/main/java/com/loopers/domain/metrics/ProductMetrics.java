package com.loopers.domain.metrics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 상품별 집계 메트릭
 * - 좋아요 수, 조회 수, 판매량 등을 실시간 집계
 * - Kafka Consumer가 이벤트를 받아서 업데이트
 * - event_handled 테이블로 멱등성 보장 (중복 처리 방지)
 */
@Entity
@Table(name = "product_metrics", indexes = {
    @Index(name = "idx_last_updated", columnList = "last_updated"),
    @Index(name = "idx_like_count", columnList = "like_count"),
    @Index(name = "idx_sales_count", columnList = "sales_count")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics {

    /**
     * 상품 ID (PK)
     */
    @Id
    @Column(name = "product_id")
    private Long productId;

    /**
     * 좋아요 수
     */
    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    /**
     * 조회 수 (상세 페이지)
     */
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    /**
     * 판매 수량
     */
    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0;

    /**
     * 판매 금액
     */
    @Column(name = "sales_amount", nullable = false)
    private long salesAmount = 0;

    /**
     * 마지막 업데이트 시각
     */
    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    /**
     * 상품 메트릭 초기 생성
     */
    public static ProductMetrics create(Long productId) {
        ProductMetrics metrics = new ProductMetrics();
        metrics.productId = productId;
        metrics.likeCount = 0;
        metrics.viewCount = 0;
        metrics.salesCount = 0;
        metrics.salesAmount = 0;
        metrics.lastUpdated = ZonedDateTime.now();
        return metrics;
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
        this.lastUpdated = ZonedDateTime.now();
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
        this.lastUpdated = ZonedDateTime.now();
    }

    /**
     * 조회 수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
        this.lastUpdated = ZonedDateTime.now();
    }

    /**
     * 판매 데이터 추가
     * @param quantity 판매 수량
     * @param amount 판매 금액
     */
    public void addSales(int quantity, long amount) {
        this.salesCount += quantity;
        this.salesAmount += amount;
        this.lastUpdated = ZonedDateTime.now();
    }

    /**
     * 모든 메트릭 초기화 (테스트용)
     */
    public void reset() {
        this.likeCount = 0;
        this.viewCount = 0;
        this.salesCount = 0;
        this.salesAmount = 0;
        this.lastUpdated = ZonedDateTime.now();
    }
}
