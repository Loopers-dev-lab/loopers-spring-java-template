package com.loopers.interfaces.api.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;


    @GetMapping(Uris.Product.GET_LIST)
    @Override
    public ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getProducts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String productName
    ) {
        ProductSearchFilter filter = new ProductSearchFilter(brandId, productName, pageable);
        Page<ProductInfo> products = productFacade.getProducts(filter);
        Page<ProductV1Dtos.ProductListResponse> responsePage = products.map(ProductV1Dtos.ProductListResponse::from);
        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping(Uris.Product.GET_DETAIL)
    @Override
    public ApiResponse<ProductV1Dtos.ProductDetailResponse> getProductDetail(
            @PathVariable Long productId,
            @RequestHeader(value = "X-USER-ID", required = false) String username
    ) {
        ProductDetailInfo productDetail = productFacade.getProductDetail(productId, username);
        ProductV1Dtos.ProductDetailResponse response = ProductV1Dtos.ProductDetailResponse.from(productDetail);
        return ApiResponse.success(response);
    }
}

