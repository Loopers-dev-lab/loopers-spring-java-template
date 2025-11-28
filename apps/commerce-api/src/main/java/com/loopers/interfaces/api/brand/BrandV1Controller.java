package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandFacade brandFacade;

    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(@PathVariable("brandId") Long brandId) {
        BrandInfo brandInfo = brandFacade.getBrand(brandId);
        return ApiResponse.success(BrandV1Dto.BrandResponse.from(brandInfo));
    }
}
