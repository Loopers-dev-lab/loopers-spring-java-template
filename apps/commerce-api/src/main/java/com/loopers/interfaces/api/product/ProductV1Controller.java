package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductService;
import com.loopers.domain.product.Product;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductService productService;

    @PostMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> createProduct(
        @Valid @RequestBody ProductV1Dto.CreateProductRequest request
    ) {
        Product product = productService.createProduct(
            request.brandId(),
            request.name(),
            request.price(),
            request.stock(),
            request.description()
        );
        ProductInfo productInfo = ProductInfo.from(product);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(productInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @PathVariable Long productId
    ) {
        Product product = productService.getProduct(productId);
        ProductInfo productInfo = ProductInfo.from(product);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(productInfo);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<Page<ProductV1Dto.ProductResponse>> getProducts(
        Pageable pageable
    ) {
        Page<Product> products = productService.getProducts(pageable);
        Page<ProductV1Dto.ProductResponse> response = products
            .map(ProductInfo::from)
            .map(ProductV1Dto.ProductResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/brands/{brandId}")
    @Override
    public ApiResponse<Page<ProductV1Dto.ProductResponse>> getProductsByBrand(
        @PathVariable Long brandId,
        Pageable pageable
    ) {
        Page<Product> products = productService.getProductsByBrand(brandId, pageable);
        Page<ProductV1Dto.ProductResponse> response = products
            .map(ProductInfo::from)
            .map(ProductV1Dto.ProductResponse::from);
        return ApiResponse.success(response);
    }

    @PutMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody ProductV1Dto.UpdateProductRequest request
    ) {
        Product product = productService.updateProduct(
            productId,
            request.name(),
            request.price(),
            request.stock(),
            request.description()
        );
        ProductInfo productInfo = ProductInfo.from(product);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(productInfo);
        return ApiResponse.success(response);
    }
}
