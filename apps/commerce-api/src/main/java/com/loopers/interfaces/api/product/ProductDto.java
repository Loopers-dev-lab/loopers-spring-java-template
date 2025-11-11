package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

import java.util.List;
import java.util.Map;

public class ProductDto {

    public record ProductListResponse(
            List<ProductResponse> products,
            int totalCount
    ) {
        public static ProductListResponse from(
                List<Product> products,
                Map<Long, Brand> brandMap
        ) {
            List<ProductResponse> productResponses = products.stream()
                    .map(product -> ProductResponse.from(product, brandMap.get(product.getBrandId())))
                    .toList();

            return new ProductListResponse(productResponses, productResponses.size());
        }
    }

    public record ProductResponse(
            Long id,
            String name,
            int price,
            BrandSummary brand
    ) {
        public static ProductResponse from(Product product, Brand brand) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    BrandSummary.from(brand)
            );
        }
    }

    public record BrandSummary(
            Long id,
            String name
    ) {
        public static BrandSummary from(Brand brand) {
            return new BrandSummary(
                    brand.getId(),
                    brand.getName()
            );
        }
    }
}
