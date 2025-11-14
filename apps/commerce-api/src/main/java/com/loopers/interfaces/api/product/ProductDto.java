package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductWithBrand;

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

        public static ProductListResponse from(List<ProductWithBrand> productsWithBrand) {
            List<ProductResponse> productResponses = productsWithBrand.stream()
                    .map(pwb -> ProductResponse.from(pwb.getProduct(), pwb.getBrand()))
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

    public record ProductDetailResponse(
            Long id,
            String name,
            String description,
            int price,
            Long stock,
            Long totalLikes,
            BrandSummary brand,
            Boolean isLiked
    ) {
        // 로그인 안 한 경우
        public static ProductDetailResponse from(Product product, Brand brand) {
            return new ProductDetailResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock(),
                    product.getTotalLikes(),
                    BrandSummary.from(brand),
                    null
            );
        }

        // 로그인 한 경우
        public static ProductDetailResponse from(Product product, Brand brand, boolean isLiked) {
            return new ProductDetailResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock(),
                    product.getTotalLikes(),
                    BrandSummary.from(brand),
                    isLiked
            );
        }

        public static ProductDetailResponse from(ProductWithBrand productWithBrand) {
            return from(productWithBrand.getProduct(), productWithBrand.getBrand());
        }

        public static ProductDetailResponse from(ProductWithBrand productWithBrand, boolean isLiked) {
            return from(productWithBrand.getProduct(), productWithBrand.getBrand(), isLiked);
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
