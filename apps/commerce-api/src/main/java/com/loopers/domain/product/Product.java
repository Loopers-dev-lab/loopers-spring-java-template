package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    private String name;
    private int price;
    private Long brandId;

    protected Product() {
    }

    private Product(String name, int price, Long brandId) {
        this.name = name;
        this.price = price;
        this.brandId = brandId;
    }

    public static Product create(String name, int price, Long brandId) {
        return new Product(name, price, brandId);
    }

    @Override
    protected void guard() {
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public Long getBrandId() {
        return brandId;
    }
}
