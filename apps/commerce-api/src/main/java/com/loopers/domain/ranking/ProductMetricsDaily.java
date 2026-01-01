package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics_daily",
    indexes = {
        @Index(name = "idx_daily_period_yyyymmdd", columnList = "period_yyyymmdd"),
        @Index(name = "idx_daily_product_date", columnList = "productId, period_yyyymmdd")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_daily", columnNames = {"productId", "period_yyyymmdd"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsDaily extends BaseEntity {

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false)
  private Integer likeCount = 0;

  @Column(nullable = false)
  private Integer orderCount = 0;

  @Column(nullable = false)
  private Integer viewCount = 0;

  @Column(nullable = false, length = 8, name = "period_yyyymmdd")
  private String yearMonthDay;

  public ProductMetricsDaily(Long productId, String yearMonthDay) {
    this.productId = productId;
    this.yearMonthDay = yearMonthDay;
  }

  public ProductMetricsDaily(Long productId, Integer likeCount, Integer orderCount, Integer viewCount, String yearMonthDay) {
    this.productId = productId;
    this.likeCount = likeCount;
    this.orderCount = orderCount;
    this.viewCount = viewCount;
    this.yearMonthDay = yearMonthDay;
  }
}
