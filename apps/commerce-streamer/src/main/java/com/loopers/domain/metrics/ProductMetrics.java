package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "product_metrics",
    indexes = {
        @Index(name = "idx_product_bucket", columnList = "productId, bucketTime"),
        @Index(name = "idx_bucket_time", columnList = "bucketTime")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_bucket", columnNames = {"productId", "bucketTime"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics extends BaseEntity {

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false)
  private String bucketTimeKey;

  @Column(nullable = false)
  private Long viewCount = 0L;

  @Column(nullable = false)
  private Long likeCount = 0L;

  @Column(nullable = false)
  private Long salesRevenue = 0L;

  public ProductMetrics(Long productId, String bucketTime) {
    this.productId = productId;
    this.bucketTimeKey = bucketTime;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }

  public void incrementLikeCount() {
    this.likeCount++;
  }

  public void decrementLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  public void incrementSalesRevenue(Long revenue) {
    this.salesRevenue += revenue;
  }
}
