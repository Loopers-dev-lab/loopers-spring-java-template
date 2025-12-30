package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics_monthly",
    indexes = {
        @Index(name = "idx_monthly_period_yyyymm", columnList = "period_yyyymm"),
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsMonthly extends BaseEntity {

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false)
  private Integer likeCount = 0;

  @Column(nullable = false)
  private Integer orderCount = 0;

  @Column(nullable = false)
  private Integer viewCount = 0;

  @Column(nullable = false, length = 6, name = "period_yyyymm")
  private String yearMonth;

  public ProductMetricsMonthly(Long productId, String yearMonth) {
    this.productId = productId;
    this.yearMonth = yearMonth;
  }

  public ProductMetricsMonthly(Long productId, Integer likeCount, Integer orderCount, Integer viewCount, String yearMonth) {
    this.productId = productId;
    this.likeCount = likeCount;
    this.orderCount = orderCount;
    this.viewCount = viewCount;
    this.yearMonth = yearMonth;
  }
}
