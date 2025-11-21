package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandDto.BrandViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandController implements BrandApiSpec {

  private final BrandFacade brandFacade;

  @Override
  @GetMapping("/{brandId}")
  public ApiResponse<BrandViewResponse> retrieveBrand(@PathVariable("brandId") Long brandId) {
    BrandResult result = brandFacade.retrieveBrand(brandId);
    return ApiResponse.success(BrandViewResponse.from(result));
  }
}
