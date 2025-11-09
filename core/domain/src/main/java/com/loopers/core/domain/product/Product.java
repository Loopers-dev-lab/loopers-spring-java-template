package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.vo.*;
import lombok.Getter;

@Getter
public class Product {

    private final ProductId productId;

    private final BrandId brandId;

    private final ProductName name;

    private final ProductPrice price;

    private final ProductStock stock;

    private final ProductLikeCount likeCount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Product(
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
        this.productId = productId;
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
