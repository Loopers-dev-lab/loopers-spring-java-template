package com.loopers.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * commerce-api의 상품 조회 API 응답 DTO
 *
 * Feign Client에서 사용
 */
public class ProductDetailExternalDto {

    /**
     * ApiResponse 래퍼 (commerce-api의 공통 응답 형식)
     */
    public record ApiResponse<T>(
            Metadata meta,
            T data
    ) {
        public record Metadata(
                String result,
                String errorCode,
                String message
        ) {}
    }

    /**
     * 상품 상세 정보 (API 응답의 data 부분)
     *
     * commerce-api의 ProductV1DTO.ProductDetailResponse와 동일한 구조
     */
    public record ProductDetailResponse(
            Long id,
            String productCode,
            String productName,
            BigDecimal price,
            Integer stock,
            Long likeCount,
            BrandResponse brand
    ) {
        /**
         * 재고 수량 조회
         */
        public int getStockQuantity() {
            return stock != null ? stock : 0;
        }
    }

    /**
     * 브랜드 정보
     */
    public record BrandResponse(
            Long id,
            String brandName,
            Boolean isActive
    ) {}
}
