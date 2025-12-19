package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.CardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping
    public ResponseEntity<PaymentResponse> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PaymentRequest request
    ) {
        log.info("[Payment API] 결제 요청 - userId: {}, orderId: {}", userId, request.orderId());

        PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
                request.orderId(),
                request.cardType(),
                request.cardNo(),
                request.amount(),
                request.callbackUrl()
        );

        PaymentInfo paymentInfo = paymentFacade.requestPayment(userId, command);

        return ResponseEntity.ok(PaymentResponse.from(paymentInfo));
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderNo(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable String orderNo
    ) {
        log.info("[Payment API] 결제 조회 - userId: {}, orderNo: {}", userId, orderNo);

        PaymentInfo paymentInfo = paymentFacade.getPaymentByOrderNo(orderNo);

        return ResponseEntity.ok(PaymentResponse.from(paymentInfo));
    }

    @PostMapping("/{transactionKey}/sync")
    public ResponseEntity<PaymentResponse> syncPaymentStatus(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable String transactionKey
    ) {
        log.info("[Payment API] 상태 동기화 - userId: {}, transactionKey: {}", userId, transactionKey);

        PaymentInfo paymentInfo = paymentFacade.syncPaymentStatus(userId, transactionKey);

        return ResponseEntity.ok(PaymentResponse.from(paymentInfo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable String orderNo,
            @RequestBody CancelRequest request
    ) {
        log.info("[Payment API] 결제 취소 - userId: {}, orderNo: {}", userId, orderNo);

        PaymentInfo paymentInfo = paymentFacade.cancelPayment(userId, orderNo, request.reason());

        return ResponseEntity.ok(PaymentResponse.from(paymentInfo));
    }

    public record PaymentRequest(
            String orderId,
            CardType cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {}

    public record CancelRequest(
            String reason
    ) {}

    public record PaymentResponse(
            Long id,
            String orderNo,
            String transactionKey,
            String status,
            String cardType,
            String cardNo,
            Long amount,
            Long pointUsed,
            String reason,
            boolean requiresRetry
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                    info.id(),
                    info.orderNo(),
                    info.transactionKey(),
                    info.status().name(),
                    info.cardType().name(),
                    info.cardNo(),
                    info.amount(),
                    info.pointUsed(),
                    info.reason(),
                    info.requiresRetry()
            );
        }
    }
}
