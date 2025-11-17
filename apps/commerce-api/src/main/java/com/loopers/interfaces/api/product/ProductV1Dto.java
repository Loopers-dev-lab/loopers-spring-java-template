package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import java.util.List;

public class ProductV1Dto {

    public record ProductListResponse(
            List<ProductItem> products
    ) {
        public static ProductListResponse from(List<ProductInfo> productInfos) {
            List<ProductItem> items = productInfos.stream()
                    .map(ProductItem::from)
                    .toList();
            return new ProductListResponse(items);
        }
    }

    public record ProductItem(
            Long id,
            String name,
            Long brandId,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer stock
    ) {
        public static ProductItem from(ProductInfo info) {
            return new ProductItem(
                    info.id(),
                    info.name(),
                    info.brandId(),
                    info.brandName(),
                    info.price(),
                    info.likeCount(),
                    info.stock()
            );
        }
    }

    public record ProductResponse(
            Long id,
            String name,
            Long brandId,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer stock
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                    info.id(),
                    info.name(),
                    info.brandId(),
                    info.brandName(),
                    info.price(),
                    info.likeCount(),
                    info.stock()
            );
        }
    }
}
