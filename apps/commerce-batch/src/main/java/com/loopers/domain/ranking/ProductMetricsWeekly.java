package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics_weekly",
    indexes = {
        @Index(name = "idx_weekly_period_yyyyww", columnList = "period_yyyyww"),
        @Index(name = "idx_weekly_product_week", columnList = "productId, period_yyyyww")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_week", columnNames = {"productId", "period_yyyyww"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsWeekly extends BaseEntity {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Integer orderCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false, length = 6, name = "period_yyyyww")
    private String yearMonthWeek;

    public ProductMetricsWeekly(Long productId, String yearMonthWeek) {
        this.productId = productId;
        this.yearMonthWeek = yearMonthWeek;
    }

    public ProductMetricsWeekly(Long productId, Integer likeCount, Integer orderCount, Integer viewCount, String yearMonthWeek) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.viewCount = viewCount;
        this.yearMonthWeek = yearMonthWeek;
    }
}
