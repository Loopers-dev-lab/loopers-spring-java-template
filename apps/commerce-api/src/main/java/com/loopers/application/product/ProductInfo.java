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
        Long id,
        String name,
        String brandName,
        Long price,
        Long likeCount
) {
    public static ProductInfo of(Product product, Brand brand, Long likeCount) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                brand.getName(),
                product.getPrice(),
                likeCount
        );
    }
}
