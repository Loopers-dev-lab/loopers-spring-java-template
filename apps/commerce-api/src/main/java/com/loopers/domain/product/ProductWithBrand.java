package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;

public class ProductWithBrand {

    private final Product product;
    private final Brand brand;

    public ProductWithBrand(Product product, Brand brand) {
        this.product = product;
        this.brand = brand;
    }

    public Product getProduct() {
        return product;
    }

    public Brand getBrand() {
        return brand;
    }
}
