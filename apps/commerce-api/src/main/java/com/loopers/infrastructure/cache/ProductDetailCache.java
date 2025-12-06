package com.loopers.infrastructure.cache;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.interfaces.api.product.ProductDto.BrandSummary;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductDetailCache {

    private String name;
    private String description;
    private long price;
    private Long stock;
    private Long totalLikes;
    private BrandSummary brand;

    private ProductDetailCache(String name, String description, long price, Long stock, Long totalLikes, BrandSummary brand) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.totalLikes = totalLikes;
        this.brand = brand;
    }

    public static ProductDetailCache from(Product product, Brand brand) {
        return new ProductDetailCache(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getTotalLikes(),
                BrandSummary.from(brand)
        );
    }
}