package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product V1 API", description = "Loopers 상품 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "상품 목록을 페이징하여 조회합니다. page, size, sort, brandId 파라미터를 사용할 수 있습니다. sort 옵션: latest(최신순), price_asc(가격 오름차순), likes_desc(좋아요 수 내림차순)"
    )
    ApiResponse<ProductV1Dto.ProductsResponse> getProducts(
        @Parameter(name = "sort", description = "정렬 옵션 (latest, price_asc, likes_desc)", required = false)
        String sort,
        @Parameter(name = "brandId", description = "브랜드 이름으로 필터링 (brandId는 브랜드 이름을 의미)", required = false)
        String brandId,
        @Parameter(name = "pageable", description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기)", required = false)
        Pageable pageable
    );

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 ID로 상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @Parameter(name = "id", description = "조회할 상품의 ID", required = true)
        Long id
    );

    @Operation(
        summary = "상품 재고 조회",
        description = "상품 ID로 상품 재고를 조회합니다."
    )
    ApiResponse<ProductV1Dto.QuantityResponse> getQuantity(
        @Parameter(name = "id", description = "조회할 상품의 ID", required = true)
        Long id
    );
}

