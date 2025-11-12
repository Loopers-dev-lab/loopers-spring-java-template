package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointDto.ChargeRequest;
import com.loopers.interfaces.api.point.PointDto.ChargeResponse;
import com.loopers.interfaces.api.point.PointDto.PointResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Point API", description = "포인트 API 입니다.")
public interface PointApiSpec {

    @Operation(
        summary = "포인트 조회",
        description = "사용자의 포인트를 조회합니다."
    )
    ApiResponse<PointResponse> getPoint(
        @Parameter(description = "사용자 로그인 ID", required = true)
        @RequestHeader(ApiHeaders.USER_LOGIN_ID) String loginId
    );

    @Operation(
        summary = "포인트 충전",
        description = "사용자의 포인트를 충전합니다."
    )
    ApiResponse<ChargeResponse> chargePoint(
        @Parameter(description = "사용자 로그인 ID", required = true)
        @RequestHeader(ApiHeaders.USER_LOGIN_ID) String loginId,
        @RequestBody(description = "충전 요청", required = true)
        ChargeRequest request
    );

}
