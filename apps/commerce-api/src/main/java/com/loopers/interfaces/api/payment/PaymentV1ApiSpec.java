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
            description = """
                    포인트로 주문을 결제합니다.
                    
                    **특징:**
                    - 동기 처리 (즉시 완료)
                    - 멱등성 보장 (idempotencyKey 사용)
                    - 실패 시 즉시 예외 응답
                    
                    **처리 흐름:**
                    1. 중복 요청 검증 (idempotencyKey)
                    2. 주문 조회 및 검증
                    3. 포인트 결제 처리
                    4. 주문 상태 COMPLETED로 변경
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PaymentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (주문 상태 오류, 포인트 부족 등)",
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
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "포인트 결제 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.PointPaymentRequest.class))
            )
            @RequestBody PaymentV1Dto.PointPaymentRequest request
    );

    @Operation(
            summary = "PG 카드 결제",
            description = """
                    PG 시스템을 통해 카드 결제를 요청합니다.
                    
                    **특징:**
                    - 비동기 처리 (콜백 대기)
                    - Circuit Breaker, Retry, Timeout 적용
                    - 멱등성 보장 (idempotencyKey 사용)
                    
                    **처리 흐름:**
                    1. 중복 요청 검증
                    2. 주문 조회 및 검증
                    3. PG 시스템에 결제 요청
                    4. 주문 상태 PAYMENT_PENDING으로 변경
                    5. 콜백 대기 (또는 스케줄러가 상태 확인)
                    
                    **장애 처리:**
                    - PG 요청 실패 시: TIMEOUT 처리 + 보상 트랜잭션
                    - 스케줄러가 30초마다 PENDING 결제 확인
                    """
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
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카드 결제 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentV1Dto.CardPaymentRequest.class))
            )
            @RequestBody PaymentV1Dto.CardPaymentRequest request
    );

    @Operation(
            summary = "PG 결제 콜백",
            description = """
                    PG 시스템에서 결제 완료 시 호출하는 엔드포인트입니다.
                    
                    **주의:**
                    - 사용자가 직접 호출하는 API가 아닙니다.
                    - PG 시스템에서 비동기로 호출합니다.
                    - 항상 200 OK 응답 (중복 재전송 방지)
                    
                    **처리 내용:**
                    - Payment 상태 업데이트
                    - Order 상태 업데이트 (PAYMENT_PENDING → COMPLETED)
                    - 실패 시: 보상 트랜잭션 이벤트 발행
                    
                    **멱등성 보장:**
                    - 이미 처리된 결제는 스킵
                    - transactionId 기준으로 중복 검증
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신 완료 (처리 성공 여부와 무관하게 200 반환)"
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
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

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
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long orderId
    );
}
