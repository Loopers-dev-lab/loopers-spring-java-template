package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.Product;

import java.math.BigDecimal;

public class ProductV1Dto {
    public record ProductResponse(Long id, Long brandId, String name, BigDecimal price, int stock, int likeCount) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                    info.id(),
                    info.brandId(),
                    info.name(),
                    info.price(),
                    info.stock(),
                    info.likeCount()
            );
        }
    }

    public record ProductRequest(Long brandId, String name, BigDecimal price, int stock) {
        public Product toEntity() {
            return new Product(
                    brandId,
                    name,
                    price,
                    stock
            );
        }
    }

    public record SearchProductRequest(String sortBy, String order) {

    }
}
