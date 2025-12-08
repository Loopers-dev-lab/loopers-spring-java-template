package com.loopers.interfaces.api.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1ApiSpec {
    private final PaymentFacade paymentFacade;

    /**
     * PG로부터 결제 결과 콜백 수신
     */
    @PostMapping(Uris.Payment.CALLBACK)
    public ApiResponse<Boolean> handleCallback(
            @RequestBody PaymentV1Dtos.PgCallbackRequest request
    ) {
        log.info("PG 콜백 수신 - transactionKey: {}, status: {}",
                request.transactionKey(), request.status());

        paymentFacade.handlePaymentCallback(request);

        return ApiResponse.success(Boolean.TRUE);
    }

}
