package com.loopers.interfaces.api.catalog;

import com.loopers.application.catalog.CatalogProductFacade;
import com.loopers.application.catalog.ProductInfo;
import com.loopers.application.catalog.ProductInfoList;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 조회 API v1 컨트롤러.
 * <p>
 * 상품 목록 조회 및 상품 정보 조회 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller {

    private final CatalogProductFacade catalogProductFacade;

    /**
     * Retrieve a paginated list of products, optionally filtered by brand and ordered by the specified sort mode.
     *
     * @param brandId optional brand ID to filter products
     * @param sort sort mode; valid values include "latest", "price_asc", "likes_desc"
     * @param page zero-based page index
     * @param size number of products per page
     * @return ApiResponse containing a ProductsResponse with the requested page of products
     */
    @GetMapping
    public ApiResponse<ProductV1Dto.ProductsResponse> getProducts(
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false, defaultValue = "latest") String sort,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        ProductInfoList result = catalogProductFacade.getProducts(brandId, sort, page, size);
        return ApiResponse.success(ProductV1Dto.ProductsResponse.from(result));
    }

    /**
     * Retrieve product information by ID.
     *
     * @param productId the ID of the product to retrieve
     * @return an ApiResponse containing the product data as a ProductV1Dto.ProductResponse
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(@PathVariable Long productId) {
        ProductInfo productInfo = catalogProductFacade.getProduct(productId);
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(productInfo));
    }
}
