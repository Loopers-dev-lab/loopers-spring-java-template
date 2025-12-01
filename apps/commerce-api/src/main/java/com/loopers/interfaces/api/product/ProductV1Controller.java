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
        // as-is : MV 적용 전
        // Page<ProductV1Dto.ProductResponse> page = productFacade.getProductsV1(brandId, pageable, sort)
        //        .map(ProductV1Dto.ProductResponse::from);
        // to-be : MV 적용 후, 캐싱 적용 전
//        Page<ProductV1Dto.ProductResponse> page = productFacade.getProducts(brandId, pageable, sort)
//                .map(ProductV1Dto.ProductResponse::from);
        // to-be : MV 적용 후, 캐싱 적용 후
        Page<ProductV1Dto.ProductResponse> page = productFacade.getProductsWithCache(brandId, pageable, sort)
                .map(ProductV1Dto.ProductResponse::from);
        return ApiResponse.success(ProductV1Dto.PageResponse.from(page));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
            @PathVariable(value = "productId") Long productId
    ) {
        // as-is : 캐시 적용 전
//         ProductDetailInfo detailInfo = productFacade.getProductDetail(productId);
        // to-be : 캐시 적용 후
        ProductDetailInfo detailInfo = productFacade.getProductDetailWithCache(productId);

        ProductV1Dto.ProductDetailResponse response = ProductV1Dto.ProductDetailResponse.from(detailInfo);
        return ApiResponse.success(response);
    }
}
