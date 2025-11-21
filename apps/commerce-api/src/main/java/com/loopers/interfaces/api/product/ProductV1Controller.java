package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {
    private final ProductFacade productFacade;

    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<ProductV1Dto.ProductsPageResponse> getProductList(@PageableDefault(size = 20) Pageable pageable) {
        Page<ProductInfo> products = productFacade.getProductList(pageable);
        ProductV1Dto.ProductsPageResponse response = ProductV1Dto.ProductsPageResponse.from(products);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProductDetail(@PathVariable Long productId) {
        ProductInfo info = productFacade.getProductDetail(productId);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(info);
        return ApiResponse.success(response);
    }
}
