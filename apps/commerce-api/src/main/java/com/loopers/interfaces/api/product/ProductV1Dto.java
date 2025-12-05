package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public class ProductV1Dto {
    public record ProductResponse(
            Long id,
            String name,
            String brand,
            int price,
            int likes,
            int stock
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                    info.id(),
                    info.name(),
                    info.brand(),
                    info.price(),
                    info.likes(),
                    info.stock()
            );
        }
    }

    public record ProductsPageResponse(
            List<ProductResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {
        public static ProductsPageResponse from(Page<ProductInfo> page) {
            return new ProductsPageResponse(
                    page.map(ProductResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getNumberOfElements()
            );
        }
    }
}
