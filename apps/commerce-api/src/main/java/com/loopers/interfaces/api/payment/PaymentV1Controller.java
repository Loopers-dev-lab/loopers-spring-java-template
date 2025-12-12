package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller implements PaymentV1Spec {
    private final PaymentFacade paymentFacade;

    @PostMapping
    @Override
    public ApiResponse<PaymentV1Dto.TransactionResponse> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PaymentV1Dto.PaymentRequest request
    ) {
        PaymentV1Dto.TransactionResponse response = paymentFacade.requestPayment(userId, request);

        return ApiResponse.success(response);
    }

}
