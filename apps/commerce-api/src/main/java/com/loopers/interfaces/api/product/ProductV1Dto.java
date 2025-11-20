package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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

    public record SearchProductRequest(SortCondition sortCondition) {
        public record SortCondition(String sortBy, String order) {
            public void conditionValidate() {
                if (!sortBy.equals("price") && !sortBy.equals("likeCount") && !sortBy.equals("createdAt")) {
                    throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 정렬 기준입니다.");
                }
                if (!order.equals("asc") && !order.equals("desc")) {
                    throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 정렬 순서입니다.");
                }
            }
        }
    }
}
