package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {
    private final BrandFacade brandFacade;
    
    @RequestMapping(method = RequestMethod.POST)
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> createBrand(@RequestBody BrandV1Dto.BrandCreateRequest request) {
        BrandInfo brandInfo = brandFacade.createBrandInfo(request.name());
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);
    }
    
    @RequestMapping(method = RequestMethod.POST, path = "/bulk")
    @Override
    public ApiResponse<BrandV1Dto.BrandBulkCreateResponse> createBrandsBulk(@RequestBody BrandV1Dto.BrandBulkCreateRequest request) {
        List<BrandInfo> brandInfos = brandFacade.createBrandInfoBulk(request.names());
        BrandV1Dto.BrandBulkCreateResponse response = BrandV1Dto.BrandBulkCreateResponse.from(brandInfos);
        return ApiResponse.success(response);
    }
    
    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<BrandV1Dto.BrandsPageResponse> getBrandList(@PageableDefault(size = 20) Pageable pageable) {
        Page<BrandInfo> brands = brandFacade.getAllBrands(pageable);
        BrandV1Dto.BrandsPageResponse response = BrandV1Dto.BrandsPageResponse.from(brands);
        return ApiResponse.success(response);
    }
}

