package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandReader;
import com.loopers.domain.brand.Brand;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandReader brandReader;

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(
            @PathVariable("brandId") Long brandId
    ) {
        Brand brand = brandReader.getBrand(brandId);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brand);
        return ApiResponse.success(response);
    }
}
