package com.loopers.application.api.productlike;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "ProductLike V1 API", description = "상품 좋아요 API 입니다.")
public interface ProductLikeV1ApiSpec {

    @Operation(
            summary = "상품 좋아요 등록",
            description = "상품에 좋아요를 등록합니다."
    )
    ApiResponse<Void> likeProduct(String productId, String userIdentifier);

    @Operation(
            summary = "상품 좋아요 취소",
            description = "상품의 좋아요를 취소합니다."
    )
    ApiResponse<Void> unlikeProduct(String productId, String userIdentifier);

    @Operation(
            summary = "좋아요 한 상품 목록 조회",
            description = "사용자가 좋아요한 상품 목록을 조회합니다."
    )
    ApiResponse<ProductLikeV1Dto.LikeProductsResponse> getLikeProducts(
            String userId,
            String brandId,
            String createdAtSort,
            String priceSort,
            String likeCountSort,
            int pageNo,
            int pageSize
    );

}
