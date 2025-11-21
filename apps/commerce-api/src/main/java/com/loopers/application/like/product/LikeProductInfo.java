package com.loopers.application.like.product;

public record LikeProductInfo(
        Long id,
        String name,
        String brand,
        int price,
        int likes,
        int stock
) {
}
