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
     * 상품 목록을 조회합니다.
     *
     * @param brandId 브랜드 ID (선택)
     * @param sort 정렬 기준 (latest, price_asc, likes_desc)
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지당 상품 수 (기본값 20)
     * @return 상품 목록을 담은 API 응답
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
     * 상품 정보를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 상품 정보를 담은 API 응답
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(@PathVariable Long productId) {
        ProductInfo productInfo = catalogProductFacade.getProduct(productId);
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(productInfo));
    }
}

