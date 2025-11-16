package com.loopers.application.api.product;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.service.product.ProductQueryService;
import com.loopers.core.service.product.query.GetProductListQuery;
import com.loopers.core.service.product.query.GetProductQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Api implements ProductV1ApiSpec {

    private final ProductQueryService queryService;

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.GetProductResponse> getProduct(@PathVariable String productId) {
        Product product = queryService.getProductBy(new GetProductQuery(productId));
        return ApiResponse.success(ProductV1Dto.GetProductResponse.from(product));
    }

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
