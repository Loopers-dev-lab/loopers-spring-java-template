package com.loopers.core.infra.database.redis.product.entity;

import com.loopers.core.domain.product.ProductDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductDetailRedisEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;
    private ProductRedisEntity product;
    private BrandRedisEntity brand;

    public static ProductDetailRedisEntity from(ProductDetail productDetail) {
        return new ProductDetailRedisEntity(
                productDetail.getProduct().getId().value(),
                ProductRedisEntity.from(productDetail.getProduct()),
                BrandRedisEntity.from(productDetail.getBrand())
        );
    }

    public ProductDetail to() {
        return new ProductDetail(
                product.to(),
                brand.to()
        );
    }
}
