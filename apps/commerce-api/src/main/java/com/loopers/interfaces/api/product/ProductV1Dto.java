package com.loopers.interfaces.api.product;

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
            int totalCount,
            int page,
            int size
    ) {
        public static ListResponse of(List<ProductItem> items, int totalCount, int page, int size) {
            boolean hasNext = (page + 1) * size < totalCount;
            return new ListResponse(items, totalCount, page, size);
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
        public static ProductItem of(Long productId, String name, BigDecimal price,
                                     Long brandId, String brandName, String imgUrl, BigDecimal likeCount,
                                     String status ,BigDecimal stock) {
            return new ProductItem(productId, name, price, brandId,brandName, imgUrl, likeCount, status, stock);
        }
    }
}
