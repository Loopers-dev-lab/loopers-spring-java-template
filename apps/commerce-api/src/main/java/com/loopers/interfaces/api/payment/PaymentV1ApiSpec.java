package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCallbackDto;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment API", description = "결제 API")
public interface PaymentV1ApiSpec {

    @Operation(
            summary = "포인트 결제",
            description = "포인트로 주문을 결제합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PaymentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/point")
    ApiResponse<PaymentV1Dto.PaymentResponse> payWithPoint(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            @RequestHeader("X-USER-ID") String loginId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "포인트 결제 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PointPaymentRequest.class))
            )
            @RequestBody PaymentV1Dto.PointPaymentRequest request
    );

    @Operation(
            summary = "PG 카드 결제",
            description = "PG 시스템을 통해 카드 결제를 요청합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "결제 요청 성공 (콜백 대기)",
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PaymentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "PG 시스템 장애",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/card")
    ApiResponse<PaymentV1Dto.PaymentResponse> payWithCard(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            @RequestHeader("X-USER-ID") String loginId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카드 결제 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.CardPaymentRequest.class))
            )
            @RequestBody PaymentV1Dto.CardPaymentRequest request
    );

    @Operation(
            summary = "PG 결제 콜백",
            description = "PG 시스템에서 결제 완료 시 호출하는 엔드포인트입니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 완료"
            )
    })
    @PostMapping("/callback")
    ResponseEntity<Void> handleCallback(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "PG 결제 결과",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentCallbackDto.class))
            )
            @RequestBody PaymentCallbackDto callback
    );

    @Operation(
            summary = "결제 정보 조회",
            description = "결제 ID로 결제 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PaymentDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "결제 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/{paymentId}")
    ApiResponse<PaymentV1Dto.PaymentDetailResponse> getPaymentDetail(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            @RequestHeader("X-USER-ID") String loginId,

            @Parameter(description = "결제 ID", required = true)
            @PathVariable Long paymentId
    );

    @Operation(
            summary = "주문별 결제 정보 조회",
            description = "주문 ID로 결제 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PaymentDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "결제 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/orders/{orderId}")
    ApiResponse<PaymentV1Dto.PaymentDetailResponse> getPaymentByOrderId(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            @RequestHeader("X-USER-ID") String loginId,

            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId
    );
}
