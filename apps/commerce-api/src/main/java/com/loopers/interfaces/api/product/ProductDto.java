package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.cache.ProductDetailCache;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public class ProductDto {

    public record ProductListResponse(
            List<ProductResponse> products,
            long totalCount
    ) {
        public static ProductListResponse from(
                Page<Product> products,
                Map<Long, Brand> brandMap
        ) {
            List<ProductResponse> productResponses = products.getContent().stream()
                    .map(product -> ProductResponse.from(product, brandMap.get(product.getBrandId())))
                    .toList();

            return new ProductListResponse(productResponses, products.getTotalElements());
        }

    }

    public record ProductResponse(
            Long id,
            String name,
            long price,
            Long totalLikes,
            BrandSummary brand
    ) {
        public static ProductResponse from(Product product, Brand brand) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getTotalLikes(),
                    BrandSummary.from(brand)
            );
        }
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            String description,
            long price,
            Long stock,
            Long totalLikes,
            BrandSummary brand,
            Boolean isLiked,
            Long rank
    ) {
        public static ProductDetailResponse from(Product product, Brand brand, Boolean isLiked, Long rank) {
            return new ProductDetailResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock(),
                    product.getTotalLikes(),
                    BrandSummary.from(brand),
                    isLiked,
                    rank
            );
        }

        public static ProductDetailResponse from(Long productId, ProductDetailCache cache, Long rank) {
            return new ProductDetailResponse(
                    productId,
                    cache.getName(),
                    cache.getDescription(),
                    cache.getPrice(),
                    cache.getStock(),
                    cache.getTotalLikes(),
                    cache.getBrand(),
                    null,
                    rank
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
