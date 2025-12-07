package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>> getProducts(
            @RequestParam(required = false) Long brandId,
            Pageable pageable,
            @RequestParam(required = false) String sort
    ) {
        Page<ProductV1Dto.ProductResponse> page = productFacade.getProducts(brandId, pageable, sort)
                .map(ProductV1Dto.ProductResponse::from);
        return ApiResponse.success(ProductV1Dto.PageResponse.from(page));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
            @PathVariable(value = "productId") Long productId
    ) {
        ProductDetailInfo detailInfo = productFacade.getProductDetail(productId);

        ProductV1Dto.ProductDetailResponse response = ProductV1Dto.ProductDetailResponse.from(detailInfo);
        return ApiResponse.success(response);
    }
}
