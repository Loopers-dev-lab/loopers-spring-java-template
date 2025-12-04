package com.loopers.interfaces.api.like;

public class ProductLikeV1Dto {

    public record ProductLikeRequest(Long productIdx) {
        public static ProductLikeRequest from(Long productIdx) {
            return new ProductLikeRequest(productIdx);
        }
    }

    public record ProductLikeResponse(
            Long id, Long productIdx, Long likeUserIdx
    ) {
        public static ProductLikeResponse from(Long id, Long productIdx, Long likeUserIdx) {
            return new ProductLikeResponse(id, productIdx, likeUserIdx);
        }
    }
}
