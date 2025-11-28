package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec{

    private final ProductFacade productFacade;

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1DTO.ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        ProductDetailInfo productDetailInfo = productFacade.getProductDetail(productId);
        ProductV1DTO.ProductDetailResponse response = ProductV1DTO.ProductDetailResponse.from(productDetailInfo);
        return ApiResponse.success(response);
    }
}
