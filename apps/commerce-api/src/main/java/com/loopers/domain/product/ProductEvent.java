package com.loopers.domain.product;

public record ProductEvent() {
    public record ProductViewed(Long productId) {
        public static ProductViewed from(Long productId) {
            return new ProductViewed(productId);
        }
    }
}
