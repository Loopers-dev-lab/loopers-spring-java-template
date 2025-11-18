package com.loopers.interfaces.api.product;

import com.loopers.domain.product.enums.ProductSortCondition;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product V1", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "검색 조건에 따라 상품 목록을 조회합니다.")
    ApiResponse<Page<ProductV1Dto.ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortCondition sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "X-USER-ID", required = false) String memberId
    );

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    ApiResponse<ProductV1Dto.ProductDetailResponse> getProductDetail(
            @PathVariable Long productId,
            @RequestHeader(value = "X-USER-ID", required = false) String memberId
    );
}