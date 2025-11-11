package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApiSpec {

    @Override
    @GetMapping
    public ApiResponse<ProductDto.ProductListResponse> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {

        ProductDto.ProductListResponse response =
                new ProductDto.ProductListResponse(List.of(), 0);

        return ApiResponse.success(response);
    }
}
