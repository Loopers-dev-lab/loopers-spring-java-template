package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApiSpec {

    private final ProductService productService;

    @Override
    @GetMapping
    public ApiResponse<ProductDto.ProductListResponse> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        ProductDto.ProductListResponse response = productService.getProducts(brandId, sort, page, size);

        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductDto.ProductDetailResponse> getProduct(
            @PathVariable Long productId
    ) {
        ProductDto.ProductDetailResponse response =
                productService.getProduct(productId);

        return ApiResponse.success(response);
    }
}
