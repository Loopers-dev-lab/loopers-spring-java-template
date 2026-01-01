package com.loopers.batch.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_metrics")
public class ProductMetrics {

  @EmbeddedId
  private ProductMetricsId id;

  @Column(name = "like_count", nullable = false)
  private Long likeCount;

  @Column(name = "sales_count", nullable = false)
  private Long salesCount;

  @Column(name = "view_count", nullable = false)
  private Long viewCount;

  @Column(name = "updated_at", nullable = false)
  private Long updatedAt;

  protected ProductMetrics() {}

  private ProductMetrics(ProductMetricsId id, Long viewCount, Long likeCount, Long salesCount) {
    this.id = id;
    this.viewCount = viewCount;
    this.likeCount = likeCount;
    this.salesCount = salesCount;
    this.updatedAt = System.currentTimeMillis();
  }

  public static ProductMetrics of(Long refProductId, Integer metricDate, Long viewCount, Long likeCount, Long salesCount) {
    return new ProductMetrics(ProductMetricsId.of(refProductId, metricDate), viewCount, likeCount, salesCount);
  }

  public Long getRefProductId() {
    return id.getRefProductId();
  }

  public Integer getMetricDate() {
    return id.getMetricDate();
  }

  public Long getLikeCount() {
    return likeCount;
  }

  public Long getSalesCount() {
    return salesCount;
  }

  public Long getViewCount() {
    return viewCount;
  }
}
