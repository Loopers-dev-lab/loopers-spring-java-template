package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BrandFacade {
  private final BrandService brandService;

  public BrandInfo getBrandDetail(long id) {
    Brand brand = brandService.getBrand(id);
    return BrandInfo.from(brand);
  }

}
