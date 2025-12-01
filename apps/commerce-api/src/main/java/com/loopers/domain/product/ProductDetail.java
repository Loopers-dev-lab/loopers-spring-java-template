package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import lombok.Getter;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductDetail
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
@Getter
public class ProductDetail {

    private Long id;
    private String name;
    private String brandName;
    private Long price;
    private Long likeCount;

    protected ProductDetail() {}

    private ProductDetail(Long id, String name, String brandName, Long price, Long likeCount) {
        this.id = id;
        this.name = name;
        this.brandName = brandName;
        this.price = price;
        this.likeCount = likeCount;
    }

    public static ProductDetail of(Product product, Brand brand, Long likeCount) {
        return new ProductDetail(
                product.getId(),
                product.getName(),
                brand.getName(),
                product.getPrice(),
                likeCount
        );
    }
}
