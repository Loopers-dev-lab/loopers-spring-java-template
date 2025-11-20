package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;

import java.util.List;

/**
 * 좋아요 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class LikeV1Dto {
    /**
     * 좋아요한 상품 목록 응답 데이터.
     *
     * @param products 좋아요한 상품 목록
     */
    public record LikedProductsResponse(
        List<LikedProductResponse> products
    ) {
        /**
         * Builds a response that contains DTOs for liked products.
         *
         * @param likedProducts the liked products from the facade to convert into DTOs
         * @return a LikedProductsResponse containing a list of LikedProductResponse mapped from the input
         */
        public static LikedProductsResponse from(List<LikeFacade.LikedProduct> likedProducts) {
            return new LikedProductsResponse(
                likedProducts.stream()
                    .map(LikedProductResponse::from)
                    .toList()
            );
        }
    }

    /**
     * 좋아요한 상품 정보 응답 데이터.
     *
     * @param productId 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param stock 상품 재고
     * @param brandId 브랜드 ID
     * @param likesCount 좋아요 수
     */
    public record LikedProductResponse(
        Long productId,
        String name,
        Integer price,
        Integer stock,
        Long brandId,
        Long likesCount
    ) {
        /**
         * Create a LikedProductResponse from a LikeFacade.LikedProduct.
         *
         * @param likedProduct the domain liked-product record to convert
         * @return a LikedProductResponse with fields mapped from the provided liked product
         */
        public static LikedProductResponse from(LikeFacade.LikedProduct likedProduct) {
            return new LikedProductResponse(
                likedProduct.productId(),
                likedProduct.name(),
                likedProduct.price(),
                likedProduct.stock(),
                likedProduct.brandId(),
                likedProduct.likesCount()
            );
        }
    }
}
