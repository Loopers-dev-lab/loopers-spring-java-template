package com.loopers.application.api.brand;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.service.brand.BrandQueryService;
import com.loopers.core.service.brand.query.GetBrandQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.loopers.application.api.brand.BrandV1Dto.GetBrandResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandV1Api implements BrandV1ApiSpec {

    private final BrandQueryService queryService;

    @Override
    @GetMapping("/{brandId}")
    public ApiResponse<GetBrandResponse> getBrand(@PathVariable String brandId) {
        Brand brand = queryService.getBrandBy(new GetBrandQuery(brandId));

        return ApiResponse.success(GetBrandResponse.from(brand));
    }
}
