package com.loopers.infrastructure.cache;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.interfaces.api.product.ProductDto.BrandSummary;
import lombok.Data;

@Data
public class ProductDetailCache {

    private String name;
    private String description;
    private long price;
    private Long stock;
    private Long totalLikes;
    private BrandSummary brand;

    public static ProductDetailCache from(Product product, Brand brand) {
        ProductDetailCache cache = new ProductDetailCache();
        cache.name = product.getName();
        cache.description = product.getDescription();
        cache.price = product.getPrice();
        cache.stock = product.getStock();
        cache.totalLikes = product.getTotalLikes();
        cache.brand = BrandSummary.from(brand);
        return cache;
    }
}