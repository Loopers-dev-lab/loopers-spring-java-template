package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.BrandService;
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

    private final BrandService brandService;

    @Override
    @GetMapping("/{brandId}")
    public ApiResponse<BrandDto.BrandDetailResponse> getBrand(
            @PathVariable Long brandId
    ) {
        BrandDto.BrandDetailResponse response = brandService.getBrand(brandId);
        return ApiResponse.success(response);
    }

}
