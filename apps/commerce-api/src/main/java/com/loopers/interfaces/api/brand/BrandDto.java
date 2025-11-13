package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

import java.util.List;

public class BrandDto {

    public record BrandDetailResponse(
            Long id,
            String name,
            List<ProductSummary> products
    ) {
        public static BrandDetailResponse from(Brand brand, List<Product> products) {
            List<ProductSummary> productSummaries = products.stream()
                    .map(ProductSummary::from)
                    .toList();

            return new BrandDetailResponse(
                    brand.getId(),
                    brand.getName(),
                    productSummaries
            );
        }
    }

    public record ProductSummary(
            Long id,
            String name,
            int price
    ) {
        public static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getPrice()
            );
        }
    }
}
