package com.loopers.interfaces.api.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductInfo;

public class ProductV1Dtos {

    @Schema(description = "상품 목록 응답")
    public record ProductListResponse(
            @Schema(description = "상품 ID", example = "1")
            Long productId,

            @Schema(description = "상품명", example = "나이키 에어맥스")
            String name,

            @Schema(description = "상품 설명", example = "최고의 운동화")
            String description,

            @Schema(description = "좋아요 수", example = "100")
            Long likeCount,

            @Schema(description = "가격 정보")
            PriceResponse price,

            @Schema(description = "브랜드 ID", example = "1")
            Long brandId,

            @Schema(description = "등록 일시")
            ZonedDateTime createdAt
    ) {
        public static ProductListResponse from(ProductInfo productInfo) {
            return new ProductListResponse(
                    productInfo.id(),
                    productInfo.name(),
                    productInfo.description(),
                    productInfo.likeCount(),
                    new PriceResponse(
                            productInfo.price().originPrice(),
                            productInfo.price().discountPrice()
                    ),
                    productInfo.brandId(),
                    productInfo.createdAt()
            );
        }
    }

    @Schema(description = "상품 상세 응답")
    public record ProductDetailResponse(
            @Schema(description = "상품 ID", example = "1")
            Long productId,

            @Schema(description = "상품명", example = "나이키 에어맥스")
            String name,

            @Schema(description = "상품 설명", example = "최고의 운동화")
            String description,

            @Schema(description = "좋아요 수", example = "100")
            Long likeCount,

            @Schema(description = "재고 수량", example = "50")
            Integer stockQuantity,

            @Schema(description = "가격 정보")
            PriceResponse price,

            @Schema(description = "브랜드 정보")
            BrandDetailResponse brand,

            @Schema(description = "사용자의 좋아요 여부", example = "true")
            Boolean isLiked
    ) {
        public static ProductDetailResponse from(ProductDetailInfo productDetailInfo) {
            return new ProductDetailResponse(
                    productDetailInfo.id(),
                    productDetailInfo.name(),
                    productDetailInfo.description(),
                    productDetailInfo.likeCount(),
                    productDetailInfo.stockQuantity(),
                    new PriceResponse(
                            productDetailInfo.price().originPrice(),
                            productDetailInfo.price().discountPrice()
                    ),
                    new BrandDetailResponse(
                            productDetailInfo.brand().id(),
                            productDetailInfo.brand().name(),
                            productDetailInfo.brand().description()
                    ),
                    productDetailInfo.isLiked()
            );
        }
    }

    @Schema(description = "가격 정보")
    public record PriceResponse(
            @Schema(description = "정가", example = "50000.00")
            BigDecimal originPrice,

            @Schema(description = "할인가 (할인이 없으면 정가와 동일)", example = "45000.00")
            BigDecimal discountPrice
    ) {
    }

    @Schema(description = "브랜드 정보")
    public record BrandResponse(
            @Schema(description = "브랜드 ID", example = "1")
            Long brandId,

            @Schema(description = "브랜드명", example = "나이키")
            String brandName
    ) {
    }

    @Schema(description = "브랜드 상세 정보")
    public record BrandDetailResponse(
            @Schema(description = "브랜드 ID", example = "1")
            Long brandId,

            @Schema(description = "브랜드명", example = "나이키")
            String brandName,

            @Schema(description = "브랜드 설명", example = "세계적인 스포츠 브랜드")
            String brandDescription
    ) {
    }
}


