package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {
    private final ProductFacade productFacade;

    @PostMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> registerProduct(@RequestBody ProductV1Dto.ProductRequest request) {
        ProductInfo info = productFacade.registerProduct(request);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(info);

        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<List<ProductV1Dto.ProductResponse>> findAllProducts() {
        List<ProductInfo> infos = productFacade.findAllProducts();

        List<ProductV1Dto.ProductResponse> responses = infos.stream()
                .map(ProductV1Dto.ProductResponse::from)
                .toList();

        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> findProductById(@PathVariable Long id) {
        ProductInfo info = productFacade.findProductById(id);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(info);

        return ApiResponse.success(response);
    }

}
