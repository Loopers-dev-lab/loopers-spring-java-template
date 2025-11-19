package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandDto.BrandViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Brand API", description = "브랜드 API 입니다.")
public interface BrandApiSpec {

  @Operation(
      summary = "브랜드 조회",
      description = "브랜드 ID로 브랜드 정보를 조회합니다."
  )
  ApiResponse<BrandViewResponse> retrieveBrand(
      @Parameter(description = "브랜드 ID", required = true)
      @PathVariable Long brandId
  );
}
