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
        /**
         * Create a ProductListResponse DTO populated from the given ProductInfo domain object.
         *
         * @param productInfo the source ProductInfo whose fields will be mapped into the DTO
         * @return a ProductListResponse containing id, name, description, like count, price, brand id, and creation timestamp copied from {@code productInfo}
         */
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
        /**
         * Create a ProductDetailResponse DTO from a ProductDetailInfo instance.
         *
         * @param productDetailInfo source product detail data to map into the DTO
         * @return a ProductDetailResponse populated with id, name, description, like count,
         *         stock quantity, price, brand details, and liked flag from the source
         */
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

    @Schema(description = "페이징 응답")
    public record PageResponse<T>(
            @Schema(description = "데이터 목록")
            List<T> content,

            @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
            int pageNumber,

            @Schema(description = "페이지 크기", example = "20")
            int pageSize,

            @Schema(description = "전체 요소 개수", example = "100")
            long totalElements,

            @Schema(description = "전체 페이지 개수", example = "5")
            int totalPages,

            @Schema(description = "첫 페이지 여부", example = "true")
            boolean first,

            @Schema(description = "마지막 페이지 여부", example = "false")
            boolean last,

            @Schema(description = "비어있는 페이지 여부", example = "false")
            boolean empty
    ) {
        /**
         * Create a PageResponse from a Spring Data Page.
         *
         * @param page the Spring Data Page to convert
         * @param <T>  the type of content in the page
         * @return a PageResponse containing the page's content and pagination metadata
         */
        public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast(),
                    page.isEmpty()
            );
        }
    }
}
