package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Point API", description = "포인트 관리 API")
public interface PointV1ApiSpec {

    @Operation(
            summary = "포인트 조회",
            description = "사용자의 보유 포인트를 조회합니다. X-USER-ID 헤더를 통해 사용자를 식별합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "포인트 조회 성공",
                    content = @Content(schema = @Schema(implementation = PointV1Dto.PointResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "X-USER-ID 헤더 누락",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "포인트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<PointV1Dto.PointResponse> getPoint(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "사용자의 포인트를 충전합니다. X-USER-ID 헤더를 통해 사용자를 식별합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "포인트 충전 성공",
                    content = @Content(schema = @Schema(implementation = PointV1Dto.PointResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "충전 요청 정보", required = true)
            @RequestBody PointV1Dto.ChargeRequest request
    );
}
