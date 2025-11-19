package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductListResponse;
import com.loopers.interfaces.api.product.ProductDto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product API", description = "상품 API 입니다.")
public interface ProductApiSpec {

  @Operation(
      summary = "상품 목록 조회",
      description = "상품 목록을 조회합니다. 브랜드 필터링, 정렬, 페이지네이션을 지원합니다."
  )
  ApiResponse<ProductListResponse> searchProductDetails(
      @Parameter(description = "사용자 ID (선택, 비회원 가능)", required = false)
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,

      @Parameter(description = "브랜드 ID (선택, 특정 브랜드 필터링)", required = false)
      @RequestParam(required = false) Long brandId,

      @Parameter(description = "정렬 기준 (latest, price_asc, likes_desc)", required = false)
      @RequestParam(defaultValue = "latest") ProductSortType sort,

      @Parameter(description = "페이지 번호 (0부터 시작)", required = false)
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "페이지당 상품 수", required = false)
      @RequestParam(defaultValue = "20") int size
  );

  @Operation(
      summary = "상품 상세 조회",
      description = "상품 ID로 상품 상세 정보를 조회합니다."
  )
  ApiResponse<ProductResponse> retrieveProductDetail(
      @Parameter(description = "사용자 ID (선택, 비회원 가능)", required = false)
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,

      @Parameter(description = "상품 ID", required = true)
      @PathVariable Long productId
  );
}
