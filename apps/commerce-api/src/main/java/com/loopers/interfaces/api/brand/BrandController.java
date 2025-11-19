package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandController implements BrandApiSpec {

    private final BrandFacade brandFacade;

    /**
     * 브랜드 정보 조회
     */
    @Override
    @GetMapping("/{brandId}")
    public ApiResponse<BrandDto.BrandDetailResponse> getBrand(
            @PathVariable Long brandId
    ) {
        BrandDto.BrandDetailResponse response = brandFacade.getBrand(brandId);
        return ApiResponse.success(response);
    }

}
