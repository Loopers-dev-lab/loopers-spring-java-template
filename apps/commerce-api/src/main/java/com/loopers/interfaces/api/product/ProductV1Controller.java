package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.ranking.RankingFacade;
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
    private final RankingFacade rankingFacade;

    @Override
    @GetMapping
    public ApiResponse<ProductV1Dto.ProductResponse> getProducts(@RequestParam(required = false) Long brandId,
                                                                 Pageable pageable,
                                                                 @RequestParam(defaultValue = "latest") String sort) {
        Page<ProductInfo> products = productFacade.getProducts(brandId, pageable, sort);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(products);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(@PathVariable Long productId) {

        ProductDetailInfo product = productFacade.getProduct(productId);
        rankingFacade.recordView(productId);
        Long rank = rankingFacade.findRank(productId, null);
        ProductV1Dto.ProductDetailResponse response = ProductV1Dto.ProductDetailResponse.from(product, rank);
        return ApiResponse.success(response);
    }
}
