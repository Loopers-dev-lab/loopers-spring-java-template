package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product API", description = "상품 조회 API")
public interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다. 브랜드 필터, 정렬, 페이징을 지원합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductV1Dto.ProductListResponse.class))
            )
    })
    @GetMapping
    ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            @Parameter(description = "브랜드 ID (필터)")
            @RequestParam(required = false) Long brandId,

            @Parameter(description = "정렬 기준 (latest, price_asc, likes_desc)")
            @RequestParam(defaultValue = "latest") String sort,

            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회합니다. 랭킹 정보가 포함됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductV1Dto.ProductDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/{productId}")
    ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable("productId") Long productId,

            @Parameter(description = "사용자 ID (조회 로그용)")
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    );
}
