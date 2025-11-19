package com.loopers.interfaces.api.like;

import com.loopers.domain.productlike.LikeSortType;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeDto.LikedProductListResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Like API", description = "상품 좋아요 API 입니다.")
public interface LikeApiSpec {

  @Operation(summary = "상품 좋아요 등록", description = "상품에 좋아요를 등록합니다.")
  ApiResponse<Object> registerProductLike(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @Parameter(description = "상품 ID", required = true)
      @PathVariable Long productId
  );

  @Operation(summary = "상품 좋아요 취소", description = "상품 좋아요를 취소합니다.")
  ApiResponse<Object> cancelProductLike(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @Parameter(description = "상품 ID", required = true)
      @PathVariable Long productId
  );

  @Operation(summary = "좋아요 상품 목록 조회", description = "사용자가 좋아요한 상품 목록을 페이지 단위로 조회합니다.")
  ApiResponse<LikedProductListResponse> retrieveLikedProducts(
      @Parameter(description = "사용자 ID", required = true)
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @Parameter(description = "정렬 (latest, product_name, price_asc, price_desc)")
      @RequestParam(defaultValue = "lastest") LikeSortType sort,
      @Parameter(description = "페이지 번호 (0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기")
      @RequestParam(defaultValue = "20") int size
  );
}
