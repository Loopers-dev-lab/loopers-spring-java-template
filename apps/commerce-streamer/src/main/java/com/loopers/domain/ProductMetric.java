package com.loopers.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductMetric extends BaseEntity {
    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "sales_volume", nullable = false)
    private Long salesVolume;

    @Column(name = "metric_date", nullable = false)
    private String metricDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ProductEventType eventType;

    public ProductMetric(Long productId, Long viewCount, Long likeCount, Long salesVolume, String metricDate, ProductEventType eventType) {
        this.productId = productId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.salesVolume = salesVolume;
        this.metricDate = metricDate;
        this.eventType = eventType;
    }

    /*
    - [ ] 리팩토링 예정
     */
    public static ProductMetric of(Long productId, String date, ProductEventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("정의되지 않은 event type입니다.");
        }

        return switch (eventType) {
            case PRODUCT_VIEWED -> ofProductViewed(productId, date);
            case PRODUCT_LIKED -> ofProductLiked(productId, date);
            case PRODUCT_SALES -> ofProductSales(productId, date);
        };
    }

    public static ProductMetric ofProductViewed(Long productId, String date) {
        return new ProductMetric(productId, 1L, 0L, 0L, date, ProductEventType.PRODUCT_VIEWED);
    }

    public static ProductMetric ofProductLiked(Long productId, String date) {
        return new ProductMetric(productId, 0L, 1L, 0L, date, ProductEventType.PRODUCT_LIKED);
    }

    public static ProductMetric ofProductSales(Long productId, String date) {
        return new ProductMetric(productId, 0L, 0L, 1L, date, ProductEventType.PRODUCT_SALES);
    }

    public void increaseProductMetric(ProductEventType eventType) {
        switch (eventType) {
            case PRODUCT_VIEWED -> increaseViewCount();
            case PRODUCT_LIKED -> increaseLikeCount();
            case PRODUCT_SALES -> increaseSalesVolume();
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void increaseSalesVolume() {
        this.salesVolume++;
    }

}
