package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;

@Tag(name = "Point API", description = "포인트 관련 API 입니다.")
public interface PointApiSpec {

    @Operation(
            summary = "포인트 조회"
            , description = "회원의 포인트 조회합니다."
    )
    ApiResponse<PointDto.PointResponse> getPoint(
            @Parameter(
                    name = "X-USER-ID"
                    , in = ParameterIn.HEADER
                    , description = "요청자 사용자 ID 헤더"
            )
            String xUserId
    );

    @Operation(
            summary = "포인트 충전"
            , description = "회원의 포인트 충전합니다."
    )
    ApiResponse<PointDto.PointResponse> charge(
            @Parameter(
                    name = "X-USER-ID"
                    , in = ParameterIn.HEADER
                    , description = "요청자 사용자 ID 헤더"
            )
            String xUserId,

            @Schema(name = "충전할 포인트", description = "충전할 포인트 액수")
            PointDto.PointRequest pointRequest
    );
}
