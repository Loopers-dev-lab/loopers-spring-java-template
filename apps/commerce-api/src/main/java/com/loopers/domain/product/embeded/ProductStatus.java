package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
@Embeddable
public class ProductStatus {

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    public ProductStatus() {}

    private ProductStatus(Status status) {
        this.status = status;
    }

    public static ProductStatus of(String productStatus) {
        Status status = Status.from(productStatus);
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Invalid product status: " + productStatus);
        }
        return new ProductStatus(status);
    }

    public boolean isAvailable() {
        return this.status.isAvailable();
    }

    public String getValue() {
        return this.status.toString();
    }

    public enum Status {
        ACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED;

        public static Status from(String status) {
            if (status == null) return null;
            try {
                return valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public boolean isAvailable() {
            return this == ACTIVE;
        }
    }
}
