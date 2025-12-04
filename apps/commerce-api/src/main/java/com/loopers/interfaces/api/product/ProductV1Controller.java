package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec{

    private final ProductFacade productFacade;

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1DTO.ProductDetailResponse> getProductDetail(
            @PathVariable Long productId
    ) {
        ProductDetailInfo productDetailInfo = productFacade.getProductDetail(productId);
        ProductV1DTO.ProductDetailResponse response = ProductV1DTO.ProductDetailResponse.from(productDetailInfo);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<ProductV1DTO.ProductsResponse> getProducts(
            @ParameterObject ProductSearchCondition condition
    ) {
        List<ProductDetailInfo> products = productFacade.getProducts(condition);
        ProductV1DTO.ProductsResponse response = ProductV1DTO.ProductsResponse.from(products);
        return ApiResponse.success(response);
    }

    /**
     * 상품 정보 수정 (캐시 무효화 포함)
     * PUT /api/v1/products/{productId}
     */
    @PutMapping("/{productId}")
    public ApiResponse<ProductV1DTO.ProductDetailResponse> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductV1DTO.UpdateProductRequest request
    ) {
        ProductDetailInfo updatedProduct = productFacade.updateProduct(
                productId,
                request.productName(),
                request.price()
        );
        ProductV1DTO.ProductDetailResponse response = ProductV1DTO.ProductDetailResponse.from(updatedProduct);
        return ApiResponse.success(response);
    }
}
