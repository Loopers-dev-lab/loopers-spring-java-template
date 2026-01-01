package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.product.vo.ProductRanking;
import lombok.Getter;

@Getter
public class ProductDetail {

    private final Product product;

    private final Brand brand;

    private final ProductRanking ranking;

    public ProductDetail(Product product, Brand brand, ProductRanking ranking) {
        this.product = product;
        this.brand = brand;
        this.ranking = ranking;
    }

    public static ProductDetail create(Product product, Brand brand) {
        return new ProductDetail(product, brand, null);
    }

    public ProductDetail with(ProductRanking ranking) {
        return new ProductDetail(product, brand, ranking);
    }
}
