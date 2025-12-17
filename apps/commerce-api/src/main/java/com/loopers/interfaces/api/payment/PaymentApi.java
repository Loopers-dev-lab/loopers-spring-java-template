package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentService;
import com.loopers.domain.payment.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Payment API", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApi {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청", description = "PG를 통한 결제를 요청합니다.")
    @PostMapping
    public ResponseEntity<PaymentResponse> requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PaymentRequest request
    ) {
        log.info("결제 요청 API 호출 - userId: {}, orderId: {}", userId, request.orderId());

        Payment payment;
        try {
            payment = paymentService.requestPayment(
                userId,
                request.orderId(),
                request.amount(),
                request.cardType(),
                request.cardNo()
            );
        } catch (Exception e) {
            log.error("결제 요청 실패, Fallback 실행 - orderId: {}, error: {}",
                request.orderId(), e.getMessage());
            // Circuit이 OPEN이거나 모든 재시도 실패 시 Fallback 결제 생성
            payment = paymentService.createFallbackPayment(
                userId,
                request.orderId(),
                request.amount(),
                request.cardType(),
                request.cardNo()
            );
        }

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @Operation(summary = "결제 콜백", description = "PG로부터 결제 결과를 받습니다.")
    @PostMapping("/callback")
    public ResponseEntity<Void> handlePaymentCallback(
        @RequestBody PaymentCallbackRequest request
    ) {
        log.info("결제 콜백 수신 - transactionKey: {}, status: {}",
            request.transactionKey(), request.status());

        paymentService.updatePaymentStatus(
            request.transactionKey(),
            request.status(),
            request.reason()
        );

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "결제 상태 동기화", description = "PG와 결제 상태를 수동으로 동기화합니다.")
    @PostMapping("/sync")
    public ResponseEntity<Void> syncPaymentStatus(
        @RequestHeader("X-USER-ID") String userId,
        @RequestParam String transactionKey
    ) {
        log.info("결제 상태 수동 동기화 요청 - transactionKey: {}", transactionKey);

        paymentService.syncPaymentStatus(userId, transactionKey);

        return ResponseEntity.ok().build();
    }
}
