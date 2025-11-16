package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Product {

    private final ProductId id;

    private final BrandId brandId;

    private final ProductName name;

    private final ProductPrice price;

    private final ProductStock stock;

    private final ProductLikeCount likeCount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private Product(
            ProductId id,
            BrandId brandId,
            ProductName name,
            ProductPrice price,
            ProductStock stock,
            ProductLikeCount likeCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Product create(
            BrandId brandId,
            ProductName name,
            ProductPrice price
    ) {
        return new Product(
                ProductId.empty(),
                brandId,
                name,
                price,
                ProductStock.init(),
                ProductLikeCount.init(),
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public static Product mappedBy(
            ProductId productId,
            BrandId brandId,
            ProductName name,
            ProductPrice price,
            ProductStock stock,
            ProductLikeCount likeCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Product(productId, brandId, name, price, stock, likeCount, createdAt, updatedAt, deletedAt);
    }

    public Product increaseLikeCount() {
        return this.toBuilder()
                .likeCount(this.likeCount.increase())
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public Product decreaseLikeCount() {
        return this.toBuilder()
                .likeCount(this.likeCount.decrease())
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public BigDecimal getTotalPrice(Quantity quantity) {
        return this.price.multiply(quantity);
    }

    public Product decreaseStock(Quantity quantity) {
        return this.toBuilder()
                .stock(this.stock.decrease(quantity))
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
