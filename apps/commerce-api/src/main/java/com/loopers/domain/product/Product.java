package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    private Product(Brand brand, String name, BigDecimal price, Integer stock,
        String description) {
        validateBrand(brand);
        validateName(name);
        validatePrice(price);
        validateStock(stock);

        this.brand = brand;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    private void validateBrand(Brand brand) {
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0개 이상이어야 합니다.");
        }
    }

    public void deductStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1개 이상이어야 합니다.");
        }
        if (this.stock < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("재고가 부족합니다. 필요: %d개, 현재: %d개", quantity, this.stock));
        }
        this.stock -= quantity;
    }

    public void restoreStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "복구 수량은 1개 이상이어야 합니다.");
        }
        this.stock += quantity;
    }

    public boolean isAvailable() {
        return this.stock > 0 && this.getDeletedAt() == null;
    }

    public void updateInfo(String name, BigDecimal price, Integer stock, String description) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
        if (stock != null) {
            validateStock(stock);
            this.stock = stock;
        }
        this.description = description;
    }
}
