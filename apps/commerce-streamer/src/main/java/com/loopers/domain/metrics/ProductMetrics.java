package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "product_metrics")
@Getter
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

    protected ProductMetrics() {
    }

    private ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.salesCount = 0L;
    }

    public static ProductMetrics initialize(Long productId) {
        return new ProductMetrics(productId);
    }

    public boolean applyLikeDelta(long delta, long version) {
        if (shouldSkip(version, lastCatalogVersion)) {
            return false;
        }
        long next = this.likeCount + delta;
        this.likeCount = Math.max(0L, next);
        this.lastCatalogVersion = version;
        return true;
    }

    public boolean increaseSales(long quantity, long version) {
        if (quantity <= 0 || shouldSkip(version, lastOrderVersion)) {
            return false;
        }
        this.salesCount = Math.max(0L, this.salesCount + quantity);
        this.lastOrderVersion = version;
        return true;
    }

    private boolean shouldSkip(long version, Long lastVersion) {
        return lastVersion != null && lastVersion >= version;
    }
}
