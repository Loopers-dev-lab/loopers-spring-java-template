package com.loopers.fixture;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;

public class ProductFixture {

    // 기본 상품 (Brand 필수)
    public static Product defaultProduct(Brand brand) {
        return Product.create("Test Product", 10000L, 100, brand);
    }

    // 가격 지정
    public static Product withPrice(Brand brand, Long price) {
        return Product.create("Product", price, 100, brand);
    }

    // 재고 지정
    public static Product withStock(Brand brand, Integer stock) {
        return Product.create("Product", 10000L, stock, brand);
    }

    // 가격 + 재고 지정
    public static Product withPriceAndStock(Brand brand, Long price, Integer stock) {
        return Product.create("Product", price, stock, brand);
    }

    // 인덱스 기반 생성
    public static Product indexed(Brand brand, int index) {
        return Product.create("Product" + index, 1000L * index, 100, brand);
    }

    // 전체 커스텀
    public static Product custom(String name, Long price, Integer stock, Brand brand) {
        return Product.create(name, price, stock, brand);
    }

    // 재고 없는 상품
    public static Product outOfStock(Brand brand) {
        return Product.create("OutOfStock Product", 10000L, 0, brand);
    }

    // 저가 상품
    public static Product cheap(Brand brand) {
        return Product.create("Cheap Product", 100L, 1000, brand);
    }

    // 고가 상품
    public static Product expensive(Brand brand) {
        return Product.create("Expensive Product", 1000000L, 10, brand);
    }
}
