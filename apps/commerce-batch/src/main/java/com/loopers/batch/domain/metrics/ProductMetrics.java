package com.loopers.batch.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product_metrics")
public class ProductMetrics {

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "last_catalog_version")
    private Long lastCatalogVersion;

    @Column(name = "last_order_version")
    private Long lastOrderVersion;
}
