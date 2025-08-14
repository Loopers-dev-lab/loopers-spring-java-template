package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductModel;
import com.loopers.infrastructure.product.ProductRepositoryImpl;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec{

    private final ProductRepositoryImpl productRepository;

    public ProductV1Controller(ProductRepositoryImpl productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @GetMapping
    public ApiResponse<ProductV1Dto.ListResponse> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Page<ProductModel> result = productRepository.search(brandId, sort, page, size);

        return ApiResponse.success(
                ProductV1Dto.ListResponse.of(
                        result.getContent(),           // 실제 데이터 목록
                        result.getTotalElements(),     // 전체 개수
                        result.getNumber(),            // 현재 페이지 번호
                        result.getSize()               // 페이지 크기
                )
        );
    }
}

