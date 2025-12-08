package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCallbackInfo;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentFacade paymentFacade;

    /**
     * PG 결제 콜백 처리
     *
     * PG에서 결제 처리 완료 후 최종 결과를 전달받는 엔드포인트
     */
    @PostMapping("/callback")
    public ApiResponse<Void> handleCallback(
            @RequestBody PaymentCallbackDto.CallbackRequest request
    ) {
        log.info("=== Payment Callback Received ===");
        log.info("TransactionKey: {}", request.transactionKey());
        log.info("Status: {}", request.status());

        // Presentation Dto → Application Dto 변환
        PaymentCallbackInfo callbackInfo = new PaymentCallbackInfo(
                request.transactionKey(),
                request.status(),
                request.reason()
        );

        // Application Layer 호출
        paymentFacade.handlePaymentCallback(callbackInfo);

        log.info("=== Payment Callback Processed Successfully ===");

        return ApiResponse.success(null);
    }
}
