package com.loopers.application.brand;

import com.loopers.domain.product.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BrandFacade {

    @Transactional(readOnly = true)
    public BrandInfo getBrand(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수입니다.");
        }
        
        Brand brand = new Brand(brandName);
        return BrandInfo.from(brand);
    }
}

