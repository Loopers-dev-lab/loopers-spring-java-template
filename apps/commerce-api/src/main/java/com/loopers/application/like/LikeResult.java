package com.loopers.application.like;

public class LikeResult {

    public record result (
            String userId,
            Long productId
    ) {
        public static result of(String userId, Long productId) {
            return new result(userId, productId);
        }
    }

}
