package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductSortType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 상품 목록 조회 검색 조건
 */
public record ProductSearchCondition(

        @Schema(description = "페이지 번호 (0부터 시작)", defaultValue = "0", example = "0")
        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기", defaultValue = "20", example = "20")
        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size,

        @Schema(description = "상품명 검색 키워드", example = "에어맥스")
        String productName,

        @Schema(description = "브랜드 ID로 필터링", example = "1")
        Long brandId,

        @Schema(description = "정렬 기준 (LATEST: 최신순, PRICE_ASC: 가격 낮은 순, LIKES_DESC: 좋아요 많은 순, BRAND: 브랜드명순, NAME: 상품명순)",
                defaultValue = "LATEST",
                example = "LIKES_DESC",
                allowableValues = {"LATEST", "PRICE_ASC", "LIKES_DESC", "BRAND", "NAME"})
        ProductSortType sortType

) {
    public ProductSearchCondition {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
        if (sortType == null) {
            sortType = ProductSortType.LATEST;
        }
    }
}
