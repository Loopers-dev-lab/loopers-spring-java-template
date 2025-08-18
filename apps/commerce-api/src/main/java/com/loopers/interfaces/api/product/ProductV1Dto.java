package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductCommand;

import java.math.BigDecimal;
import java.util.List;

public class ProductV1Dto {
    public record Request(
            Long brandId,
            SortType sort,
            int page,
            int size
    ) {
        public static Request of(Long brandId, String sort, int page, int size) {
            SortType sortType = SortType.from(sort);
            return new Request(brandId, sortType, page, size);
        }

    }
    
    public enum SortType {
        LATEST("latest"),
        PRICE_ASC("price_asc"),
        LIKES_DESC("likes_desc");
        
        private final String value;
        
        SortType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static SortType from(String value) {
            if (value == null) {
                return LATEST; // 기본값
            }
            
            for (SortType sortType : values()) {
                if (sortType.value.equalsIgnoreCase(value)) {
                    return sortType;
                }
            }
            return LATEST;
        }
    }
    
    public record ListResponse(
            List<ProductItem> items,
            long totalCount,
            int page,
            int size
    ) {
        public static ListResponse of(List<ProductCommand.ProductItem> productCommandItems, long totalCount, int page, int size) {
            List<ProductItem> productItems = productCommandItems.stream()
                    .map(ProductItem::from)
                    .toList();
            return new ListResponse(productItems, totalCount, page, size);
        }
    }
    public record ProductItem(
            Long productId,
            String name,
            BigDecimal price,
            Long brandId,
            String brandName,
            String imgUrl,
            BigDecimal likeCount,
            String status,
            BigDecimal stock
    ) {
        public static ProductItem from(ProductCommand.ProductItem productCommandItem) {
            return new ProductItem(
                    productCommandItem.productId(),
                    productCommandItem.name(),
                    productCommandItem.price(),
                    productCommandItem.brandId(),
                    productCommandItem.brandName(),
                    productCommandItem.imgUrl(),
                    productCommandItem.likeCount(),
                    productCommandItem.status(),
                    productCommandItem.stock()
            );
        }
    }
}
