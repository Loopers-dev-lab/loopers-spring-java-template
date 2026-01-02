package com.loopers.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_view_logs", indexes = {
    @Index(name = "idx_product_id_created_at", columnList = "product_id, created_at")
})
public class ProductViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProductViewLog() {
    }

    private ProductViewLog(Long productId) {
        this.productId = productId;
    }

    public static ProductViewLog create(Long productId) {
        return new ProductViewLog(productId);
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
