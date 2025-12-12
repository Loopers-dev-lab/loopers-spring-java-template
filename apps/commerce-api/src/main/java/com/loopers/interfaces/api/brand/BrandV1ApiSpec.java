package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Brand V1 API", description = "브랜드 API 입니다.")
public interface BrandV1ApiSpec {
    
    @Operation(
            method = "POST",
            summary = "브랜드 생성",
            description = "새로운 브랜드를 생성합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> createBrand(
            @Schema(
                    name = "브랜드 생성 요청",
                    description = "브랜드 이름을 입력합니다."
            )
            BrandV1Dto.BrandCreateRequest request
    );
    
    @Operation(
            method = "POST",
            summary = "브랜드 일괄 생성",
            description = "여러 브랜드를 한 번에 생성합니다."
    )
    ApiResponse<BrandV1Dto.BrandBulkCreateResponse> createBrandsBulk(
            @Schema(
                    name = "브랜드 일괄 생성 요청",
                    description = "생성할 브랜드 이름 목록을 입력합니다."
            )
            BrandV1Dto.BrandBulkCreateRequest request
    );
    
    @Operation(
            method = "GET",
            summary = "브랜드 목록 조회",
            description = "브랜드 목록을 페이지네이션으로 조회합니다."
    )
    ApiResponse<BrandV1Dto.BrandsPageResponse> getBrandList(
            @Schema(
                    name = "페이지 정보",
                    description = "페이지 번호, 페이지 크기, 정렬 정보를 포함한 페이지 정보" +
                            "\n- 기본값: page=0, size=20"
            )
            Pageable pageable
    );
}


