package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Payment V1 Controller
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller implements PaymentV1ApiSpec {

    private static final Logger log = LoggerFactory.getLogger(PaymentV1Controller.class);

    private final PaymentFacade paymentFacade;

    /**
     * 결제 요청
     */
    @PostMapping
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
            @RequestHeader(value = "X-USER-ID") String userId,
            @RequestBody PaymentV1Dto.PaymentRequest request
    ) {
        log.info("결제 요청: userId={}, orderId={}, amount={}", userId, request.getOrderId(), request.getAmount());

        PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
                userId,
                request.getOrderId(),
                request.getCardType(),
                request.getCardNo(),
                request.getAmount(),
                request.getCallbackUrl()
        );

        PaymentInfo info = paymentFacade.requestPayment(command);
        PaymentV1Dto.PaymentResponse response = PaymentV1Dto.PaymentResponse.from(info);

        return ApiResponse.success(response);
    }

    /**
     * 결제 상세 조회
     */
    @GetMapping("/{paymentId}")
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> getPayment(
            @RequestHeader(value = "X-USER-ID") String userId,
            @PathVariable("paymentId") Long paymentId
    ) {
        log.info("결제 조회: userId={}, paymentId={}", userId, paymentId);

        PaymentInfo info = paymentFacade.getPaymentDetail(paymentId, userId);
        PaymentV1Dto.PaymentResponse response = PaymentV1Dto.PaymentResponse.from(info);

        return ApiResponse.success(response);
    }

    /**
     * PG 콜백 수신
     */
    @PostMapping("/callback")
    @Override
    public ApiResponse<Void> handleCallback(
            @RequestBody PaymentV1Dto.CallbackRequest request
    ) {
        log.info("PG 콜백 수신: transactionKey={}, status={}", request.getTransactionKey(), request.getStatus());

        PaymentCommand.UpdatePaymentStatus command = new PaymentCommand.UpdatePaymentStatus(
                request.getTransactionKey(),
                request.getStatus(),
                request.getReason()
        );

        paymentFacade.handleCallback(command);

        return ApiResponse.success(null);
    }
}
