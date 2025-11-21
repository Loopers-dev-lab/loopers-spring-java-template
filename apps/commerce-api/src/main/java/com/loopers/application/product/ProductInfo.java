package com.loopers.application.product;

public record ProductInfo(
        Long id,
        String name,
        String brand,
        int price,
        int likes,
        int stock
) {
}
