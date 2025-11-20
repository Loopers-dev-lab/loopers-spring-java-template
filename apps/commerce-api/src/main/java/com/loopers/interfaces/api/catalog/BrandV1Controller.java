package com.loopers.interfaces.api.catalog;

import com.loopers.application.catalog.CatalogBrandFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 브랜드 조회 API v1 컨트롤러.
 * <p>
 * 브랜드 정보 조회 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller {

    private final CatalogBrandFacade catalogBrandFacade;

    /**
     * 브랜드 정보를 조회합니다.
     *
     * @param brandId 브랜드 ID
     * @return 브랜드 정보를 담은 API 응답
     */
    @GetMapping("/{brandId}")
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(@PathVariable Long brandId) {
        CatalogBrandFacade.BrandInfo brandInfo = catalogBrandFacade.getBrand(brandId);
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(brandInfo));
    }
}

