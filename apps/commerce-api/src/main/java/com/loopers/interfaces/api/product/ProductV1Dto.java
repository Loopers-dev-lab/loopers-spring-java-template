package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.common.Quantity;
import org.springframework.data.domain.Page;

public class ProductV1Dto {
    public record ProductResponse(Long id, String name, String brand, long price, Long likeCount) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.brand().name(),
                info.price().value(),
                info.likeCount()
            );
        }
    }

    public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
            );
        }
    }

    public record ProductsResponse(java.util.List<ProductResponse> products, PageInfo pageInfo) {
        public static ProductsResponse from(Page<ProductInfo> productPage) {
            java.util.List<ProductResponse> productResponses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();
            return new ProductsResponse(productResponses, PageInfo.from(productPage));
        }
    }

    public record QuantityResponse(Integer quantity) {
        public static QuantityResponse from(Quantity quantity) {
            return new QuantityResponse(quantity.quantity());
        }
    }
}

