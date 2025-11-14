package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class BrandFacade {

    private final BrandService brandService;

    /**
     * 브랜드 상세 조회
     */
    @Transactional(readOnly = true)
    public BrandInfo getBrand(Long brandId) {
        Brand brand = brandService.findById(brandId);
        return BrandInfo.from(brand);
    }
}

