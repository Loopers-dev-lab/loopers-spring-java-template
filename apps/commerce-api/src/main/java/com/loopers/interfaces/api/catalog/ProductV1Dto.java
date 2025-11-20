package com.loopers.interfaces.api.catalog;

import com.loopers.application.catalog.ProductInfo;
import com.loopers.application.catalog.ProductInfoList;

import java.util.List;

/**
 * 상품 조회 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class ProductV1Dto {
    /**
     * 상품 정보 응답 데이터.
     *
     * @param productId 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param stock 상품 재고
     * @param brandId 브랜드 ID
     * @param likesCount 좋아요 수
     */
    public record ProductResponse(
        Long productId,
        String name,
        Integer price,
        Integer stock,
        Long brandId,
        Long likesCount
    ) {
        /**
         * Create a ProductResponse from the given ProductInfo.
         *
         * @param productInfo the product information whose detail fields are used to populate the response
         * @return a ProductResponse populated with values extracted from productInfo
         */
        public static ProductResponse from(ProductInfo productInfo) {
            var detail = productInfo.productDetail();
            return new ProductResponse(
                detail.getId(),
                detail.getName(),
                detail.getPrice(),
                detail.getStock(),
                detail.getBrandId(),
                detail.getLikesCount()
            );
        }
    }

    /**
     * 상품 목록 응답 데이터.
     *
     * @param products 상품 목록
     * @param totalCount 전체 상품 수
     * @param page 현재 페이지 번호
     * @param size 페이지당 상품 수
     * @param totalPages 전체 페이지 수
     * @param hasNext 다음 페이지 존재 여부
     * @param hasPrevious 이전 페이지 존재 여부
     */
    public record ProductsResponse(
        List<ProductResponse> products,
        long totalCount,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
    ) {
        /**
         * Create a ProductsResponse DTO from a ProductInfoList.
         *
         * @param result source ProductInfoList containing product entries and pagination metadata
         * @return a ProductsResponse containing mapped ProductResponse entries and the copied pagination fields
         */
        public static ProductsResponse from(ProductInfoList result) {
            List<ProductResponse> productResponses = result.products().stream()
                .map(ProductResponse::from)
                .toList();

            return new ProductsResponse(
                productResponses,
                result.totalCount(),
                result.page(),
                result.size(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
            );
        }
    }
}
