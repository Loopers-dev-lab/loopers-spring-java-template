package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;

@Entity
@Table(name = "product")
public class ProductModel extends BaseEntity {

    private String name;
    @Embedded
    private Brand brand;
    @Embedded
    private Money price;
    @Embedded
    private Quantity quantity;
    private Long likeCount;

    public ProductModel(String name, Brand brand, Money price, Quantity quantity) {

        this.name = name;
        this.brand = brand;
        this.price = price;
        this.quantity = quantity;
        this.likeCount = 0L;
    }

    public String getName() {
        return name;
    }

    public Brand getBrand() {
        return brand;
    }

    public Money getPrice() {
        return price;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public void decreaseQuantity(Quantity quantityToDecrease) {
        if (this.quantity.quantity() < quantityToDecrease.quantity()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }

        this.quantity = new Quantity(this.quantity.quantity() - quantityToDecrease.quantity());

    }

}
