package com.loopers.application.api.product;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.service.product.ProductQueryService;
import com.loopers.core.service.product.query.GetProductListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Api implements ProductV1ApiSpec {

    private final ProductQueryService queryService;

    @Override
    @GetMapping("/")
    public ApiResponse<ProductV1Dto.GetProductListResponse> getProductList(
            @RequestParam String brandId,
            @RequestParam String createdAtSort,
            @RequestParam String priceSort,
            @RequestParam String likeCountSort,
            @RequestParam int pageNo,
            @RequestParam int pageSize
    ) {
        ProductListView productList = queryService.getProductList(new GetProductListQuery(
                brandId, createdAtSort, priceSort, likeCountSort, pageNo, pageSize
        ));
        return ApiResponse.success(ProductV1Dto.GetProductListResponse.from(productList));
    }
}
