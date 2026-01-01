package com.loopers.domain.metrics;

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

  private ProductMetrics(
      ProductMetricsId id, Long likeCount, Long salesCount, Long viewCount, Long updatedAt) {
    this.id = id;
    this.likeCount = likeCount;
    this.salesCount = salesCount;
    this.viewCount = viewCount;
    this.updatedAt = updatedAt;
  }

  public static ProductMetrics createWithLike(Long productId, Integer metricDate, int delta, Long occurredAt) {
    if (productId == null || metricDate == null || occurredAt == null) {
      throw new IllegalArgumentException("productId, metricDate, occurredAt은 필수입니다");
    }
    long initialLikeCount = Math.max(delta, 0);
    return new ProductMetrics(ProductMetricsId.of(productId, metricDate), initialLikeCount, 0L, 0L, occurredAt);
  }

  public ProductMetricsId getId() {
    return id;
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

  public Long getUpdatedAt() {
    return updatedAt;
  }
}
