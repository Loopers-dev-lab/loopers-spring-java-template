package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandFacade {

  private final BrandService brandService;

  public BrandResult retrieveBrand(Long brandId) {
    Brand brand = brandService.getById(brandId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    return BrandResult.from(brand);
  }
}
