package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

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
        validatePrice(price);
        validateStock(stock);

        return new Product(name, description, price, stock, brandId);
    }

    private static void validatePrice(int price) {
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }
    }

    private static void validateStock(Long stock) {
        if (Objects.isNull(stock)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 비어있을 수 없습니다.");
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

    public void decreaseStock(Long quantity) {
        if (this.stock < quantity) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST, String.format("상품 '%s'의 재고가 부족합니다.", this.name)
            );
        }

        this.stock -= quantity;
    }

    public boolean hasEnoughStock(Long quantity) {
        return this.stock >= quantity;
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
