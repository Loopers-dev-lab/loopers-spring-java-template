package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.brand.BrandService;
import com.loopers.domain.brand.Brand;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandService brandService;

    @PostMapping
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> createBrand(
        @Valid @RequestBody BrandV1Dto.CreateBrandRequest request
    ) {
        Brand brand = brandService.createBrand(request.name(), request.description());
        BrandInfo brandInfo = BrandInfo.from(brand);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(
        @PathVariable Long brandId
    ) {
        Brand brand = brandService.getBrand(brandId);
        BrandInfo brandInfo = BrandInfo.from(brand);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/search")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrandByName(
        @RequestParam String name
    ) {
        Brand brand = brandService.getBrandByName(name);
        BrandInfo brandInfo = BrandInfo.from(brand);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);
    }

    @PutMapping("/{brandId}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> updateBrand(
        @PathVariable Long brandId,
        @Valid @RequestBody BrandV1Dto.UpdateBrandRequest request
    ) {
        Brand brand = brandService.updateBrand(brandId, request.name(), request.description());
        BrandInfo brandInfo = BrandInfo.from(brand);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);
    }
}
