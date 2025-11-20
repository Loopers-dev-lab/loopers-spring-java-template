package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.Brand;
import lombok.Getter;

@Getter
public class ProductDetail {

    private final Product product;

    private final Brand brand;

    public ProductDetail(Product product, Brand brand) {
        this.product = product;
        this.brand = brand;
    }
}
