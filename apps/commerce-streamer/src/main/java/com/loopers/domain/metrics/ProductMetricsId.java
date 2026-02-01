package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProductMetricsId implements Serializable {

  @Column(name = "ref_product_id", nullable = false)
  private Long refProductId;

  @Column(name = "metric_date", nullable = false)
  private Integer metricDate;

  protected ProductMetricsId() {}

  private ProductMetricsId(Long refProductId, Integer metricDate) {
    this.refProductId = refProductId;
    this.metricDate = metricDate;
  }

  public static ProductMetricsId of(Long refProductId, Integer metricDate) {
    return new ProductMetricsId(refProductId, metricDate);
  }

  public Long getRefProductId() {
    return refProductId;
  }

  public Integer getMetricDate() {
    return metricDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProductMetricsId that = (ProductMetricsId) o;
    return Objects.equals(refProductId, that.refProductId) && Objects.equals(metricDate, that.metricDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refProductId, metricDate);
  }
}