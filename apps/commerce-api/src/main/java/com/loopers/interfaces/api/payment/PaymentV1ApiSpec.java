package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment V1 API", description = "Payment API 입니다.")
public interface PaymentV1ApiSpec {
    @Operation(
            summary = "PG callback API",
            description = "PG 요청시 callback API 입니다."
    )
    ApiResponse<PointV1Dto.PointResponse> callback(
            @Schema(name = "PG callback PaymentCallbackRequest", description = "PG callback PaymentCallbackRequest")
            PaymentV1Dto.PaymentCallbackRequest request
    );

}
