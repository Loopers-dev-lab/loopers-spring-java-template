package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandController implements BrandApiSpec {

    private final BrandFacade brandFacade;

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandDto.BrandResponse> getBrand(
            @PathVariable Long brandId
    ) {
        var brandInfo = brandFacade.getBrand(brandId);
        return ApiResponse.success(BrandDto.BrandResponse.from(brandInfo));
    }
}

