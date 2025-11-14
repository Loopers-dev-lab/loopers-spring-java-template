package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.loopers.domain.product.ProductCondition;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<Page<ProductDto.ProductResponse>> getProducts(
            @ModelAttribute ProductDto.SearchRequest request
    ) {
        // DTO → Domain 변환
        ProductCondition condition = request.toCondition();
        
        // PageRequest 생성
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );
        
        // Facade 호출 및 변환
        var productInfoPage = productFacade.getProducts(condition, pageable);

        // PageResponse로 변환
        return ApiResponse.success(productInfoPage.map(ProductDto.ProductResponse::from));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductDto.ProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        // Facade 호출 (단일 객체)
        var productInfo = productFacade.getProduct(productId);
        
        // Info → DTO 변환 후 반환
        return ApiResponse.success(ProductDto.ProductResponse.from(productInfo));
    }
}

