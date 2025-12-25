package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상품 API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

  @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다.")
  @GetMapping("/api/v1/products")
  ApiResponse<ProductV1Dto.ProductListsResponse> getProducts(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,

      @Parameter(description = "브랜드 ID 필터", example = "1")
      @RequestParam(required = false) Long brandId,
      @RequestParam(defaultValue = "0") String sortType,

      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
      @RequestParam(defaultValue = "0") int page,

      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(defaultValue = "20") int size

  );

  @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다.")
  @GetMapping("/api/v1/products/{productId}")
  ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @Parameter(description = "상품 ID", example = "1") @PathVariable Long productId
  );

}
