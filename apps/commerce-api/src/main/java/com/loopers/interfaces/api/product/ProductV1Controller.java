package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductSearchCommand;
import com.loopers.application.product.ProductSummaryInfo;
import com.loopers.domain.product.enums.ProductSortCondition;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping("/products")
    public ApiResponse<Page<ProductV1Dto.ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") ProductSortCondition sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-USER-ID", required = false) String memberId
    ) {
        ProductSearchCommand command = ProductSearchCommand.of(keyword, sort, page, size, memberId);
        Page<ProductSummaryInfo> products = productFacade.getProducts(command);
        
        Page<ProductV1Dto.ProductSummaryResponse> response = products.map(ProductV1Dto.ProductSummaryResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProductDetail(
            @PathVariable Long productId,
            @RequestHeader(value = "X-USER-ID", required = false) String memberId
    ) {
        ProductDetailInfo productDetail = productFacade.getProductDetail(productId, memberId);
        ProductV1Dto.ProductDetailResponse response = ProductV1Dto.ProductDetailResponse.from(productDetail);
        return ApiResponse.success(response);
    }
}