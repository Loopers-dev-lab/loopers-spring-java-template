package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

/**
 * packageName : com.loopers.application.product
 * fileName     : ProductInfo
 * author      : byeonsungmun
 * date        : 2025. 11. 10.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 10.     byeonsungmun       최초 생성
 */
public record ProductInfo(
        Long productId,
        String productName,
        Long brandId,
        String brandName,
        Long price,
        Long likeCount
) {
    public static ProductInfo of(Product product, Brand brand) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                brand.getId(),
                brand.getName(),
                product.getPrice(),
                product.getLikeCount()
        );
    }
}
