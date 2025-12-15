package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.*;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1ApiSpec {

    private final PaymentFacade paymentFacade;
    private final PaymentCallbackService paymentCallbackService;

    @Override
    @PostMapping("/point")
    public ApiResponse<PaymentV1Dto.PaymentResponse> payWithPoint(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody PaymentV1Dto.PointPaymentRequest request
    ) {
        PaymentPointCommand command = request.toCommand(userId);
        PaymentInfo paymentInfo = paymentFacade.payWithPoint(command);

        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(paymentInfo));
    }

    @Override
    @PostMapping("/card")
    public ApiResponse<PaymentV1Dto.PaymentResponse> payWithCard(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody PaymentV1Dto.CardPaymentRequest request
    ) {
        PaymentPgCardCommand command = request.toCommand(userId);
        PaymentInfo paymentInfo = paymentFacade.payWithPgCard(command);

        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(paymentInfo));
    }

    @Override
    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestBody PaymentCallbackDto callback
    ) {
        log.info("결제 콜백 수신: transactionId={}, status={}",
                callback.transactionId(), callback.status());

        try {
            paymentCallbackService.processCallback(callback);
        } catch (Exception e) {
            log.error("콜백 처리 실패 (200 반환): transactionId={}",
                    callback.transactionId(), e);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 결제 정보 조회
     */
    @Override
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentV1Dto.PaymentDetailResponse> getPaymentDetail(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long paymentId
    ) {
        PaymentInfo paymentInfo = paymentFacade.getPaymentInfo(paymentId);

        return ApiResponse.success(PaymentV1Dto.PaymentDetailResponse.from(paymentInfo));
    }

    /**
     * 주문별 결제 정보 조회
     */
    @Override
    @GetMapping("/orders/{orderId}")
    public ApiResponse<PaymentV1Dto.PaymentDetailResponse> getPaymentByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long orderId
    ) {
        PaymentInfo paymentInfo = paymentFacade.getPaymentInfoByOrderId(orderId);

        return ApiResponse.success(PaymentV1Dto.PaymentDetailResponse.from(paymentInfo));
    }
}
