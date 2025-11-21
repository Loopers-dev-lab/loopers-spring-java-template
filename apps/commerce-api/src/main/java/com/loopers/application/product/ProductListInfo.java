package com.loopers.application.product;

import com.loopers.domain.product.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record ProductListInfo(
        List<ProductContent> contents,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ProductListInfo of(Page<Product> productPage, Map<Long, Long> likeCountMap) {
        List<ProductContent> contents = productPage.getContent().stream()
                .map(product -> ProductContent.of(
                        product,
                        likeCountMap.getOrDefault(product.getId(), 0L))
                )
                .toList();

        return new ProductListInfo(
                contents,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public record ProductContent(
            Long id,
            String name,
            Long price,
            Long brandId,
            String brandName,
            Long likeCount
    ) {
        public static ProductContent of(Product product, Long likeCount) {
            return new ProductContent(
                    product.getId(),
                    product.getName(),
                    product.getPriceValue(),
                    product.getBrand().getId(),
                    product.getBrand().getName(),
                    likeCount
            );
        }
    }
}
