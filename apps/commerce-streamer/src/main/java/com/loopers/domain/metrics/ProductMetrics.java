package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_metrics")
public class ProductMetrics {

  @Id
  @Column(name = "ref_product_id", nullable = false)
  private Long refProductId;

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
      Long refProductId, Long likeCount, Long salesCount, Long viewCount, Long updatedAt) {
    this.refProductId = refProductId;
    this.likeCount = likeCount;
    this.salesCount = salesCount;
    this.viewCount = viewCount;
    this.updatedAt = updatedAt;
  }

  public static ProductMetrics createWithLike(Long productId, int delta, Long occurredAt) {
    long initialLikeCount = Math.max(delta, 0);
    return new ProductMetrics(productId, initialLikeCount, 0L, 0L, occurredAt);
  }

  public Long getRefProductId() {
    return refProductId;
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

  public void incrementLikeCount(Long occurredAt) {
    this.likeCount++;
    this.updatedAt = Math.max(this.updatedAt, occurredAt);
  }

  public void decrementLikeCount(Long occurredAt) {
    this.likeCount = Math.max(this.likeCount - 1, 0);
    this.updatedAt = Math.max(this.updatedAt, occurredAt);
  }
}
