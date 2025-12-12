package com.loopers.fixture;

import com.loopers.domain.brand.Brand;

public class BrandFixture {

    public static Brand defaultBrand() {
        return Brand.create("Test Brand");
    }

    public static Brand withName(String name) {
        return Brand.create(name);
    }

    public static Brand indexed(int index) {
        return Brand.create("Brand" + index);
    }

    // 실제 브랜드 예시
    public static Brand nike() {
        return Brand.create("Nike");
    }

    public static Brand adidas() {
        return Brand.create("Adidas");
    }

    public static Brand stussy() {
        return Brand.create("Stussy");
    }
}
