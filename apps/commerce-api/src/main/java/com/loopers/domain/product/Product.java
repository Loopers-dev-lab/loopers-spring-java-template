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
    private String description;
    private int price;
    private Long brandId;
    private Long stock;
    private Long totalLikes;

    protected Product() {
    }

    private Product(String name, String description, int price, Long stock, Long brandId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.totalLikes = 0L;
        this.brandId = brandId;
    }

    public static Product create(String name, String description, int price, Long stock, Long brandId) {
        return new Product(name, description, price, stock, brandId);
    }

    @Override
    protected void guard() {
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }

        if (stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 1개 이상이어야 합니다.");
        }
    }

    public void increaseLikes() {
        this.totalLikes++;
    }

    public void decreaseLikes() {
        if (this.totalLikes > 0) {
            this.totalLikes--;
        }
    }

    public boolean hasStock() {
        return this.stock > 0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public Long getBrandId() {
        return brandId;
    }

    public Long getStock() {
        return stock;
    }

    public Long getTotalLikes() {
        return totalLikes;
    }
}
