package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.common.Quantity;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductsResponse> getProducts(
        @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
        @RequestParam(value = "brandId", required = false) String brandId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Pageable sortedPageable = convertSortToPageable(sort, pageable);
        Page<ProductInfo> productPage = productFacade.getProducts(sortedPageable, sort, brandId);
        ProductV1Dto.ProductsResponse response = ProductV1Dto.ProductsResponse.from(productPage);
        return ApiResponse.success(response);
    }

    private Pageable convertSortToPageable(String sort, Pageable pageable) {
        Sort.Direction direction;
        String property;
        
        switch (sort) {
            case "latest":
                property = "id";
                direction = Sort.Direction.DESC;
                break;
            case "price_asc":
                property = "price";
                direction = Sort.Direction.ASC;
                break;
            case "likes_desc":
                // likes_desc는 메모리에서 정렬하므로 여기서는 기본 정렬 사용
                property = "id";
                direction = Sort.Direction.DESC;
                break;
            default:
                property = "id";
                direction = Sort.Direction.DESC;
        }
        
        return org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(direction, property)
        );
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @PathVariable("id") Long id
    ) {
        ProductInfo info = productFacade.getProduct(id);
        ProductV1Dto.ProductResponse response = ProductV1Dto.ProductResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}/quantity")
    @Override
    public ApiResponse<ProductV1Dto.QuantityResponse> getQuantity(
        @PathVariable("id") Long id
    ) {
        Quantity quantity = productFacade.getQuantity(id);
        ProductV1Dto.QuantityResponse response = ProductV1Dto.QuantityResponse.from(quantity);
        return ApiResponse.success(response);
    }
}

