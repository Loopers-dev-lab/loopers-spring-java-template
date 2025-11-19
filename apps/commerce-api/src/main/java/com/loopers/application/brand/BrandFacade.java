package com.loopers.application.brand;


import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.interfaces.api.brand.BrandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandFacade {

    private final BrandDomainService brandDomainService;
    private final ProductDomainService productDomainService;

    @Transactional(readOnly = true)
    public BrandDto.BrandDetailResponse getBrand(Long brandId) {
        // 브랜드 조회
        Brand brand = brandDomainService.getActiveBrand(brandId);

        // 해당 브랜드 상품 목록 조회
        List<Product> products = productDomainService.getProductsByBrandId(brandId);

        // 조립
        return BrandDto.BrandDetailResponse.from(brand, products);
    }

}
