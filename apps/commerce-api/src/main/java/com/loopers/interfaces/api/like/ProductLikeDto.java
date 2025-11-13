package com.loopers.interfaces.api.like;

public class ProductLikeDto {

    public record LikeResponse(
            boolean liked,
            long totalLikes
    ) {
        public static LikeResponse from(boolean liked, long totalLikes) {
            return new LikeResponse(liked, totalLikes);
        }
    }
}
