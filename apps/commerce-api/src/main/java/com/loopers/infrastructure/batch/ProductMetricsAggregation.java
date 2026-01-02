package com.loopers.infrastructure.batch;

public class ProductMetricsAggregation {

    private final Long productId;
    private final Long viewCount;
    private final Long likeCount;
    private final Long orderCount;
    private final Long totalQuantity;
    private final Long score;

    public ProductMetricsAggregation(Long productId, Long viewCount, Long likeCount,
                                     Long orderCount, Long totalQuantity, Long score) {
        this.productId = productId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.totalQuantity = totalQuantity;
        this.score = score;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public Long getScore() {
        return score;
    }
}
