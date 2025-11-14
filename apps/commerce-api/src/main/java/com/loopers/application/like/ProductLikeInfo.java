package com.loopers.application.like;

public record ProductLikeInfo(
        boolean liked,
        Long totalLikes
) {
    public static ProductLikeInfo from(boolean liked, Long totalLikes) {
        return new ProductLikeInfo(liked, totalLikes);
    }
}
