package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller implements PaymentV1ApiSpec {

    private final PaymentFacade paymentFacade;

    @PostMapping
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
            @RequestHeader("X-USER-ID") String loginId,
            @Valid @RequestBody PaymentV1Dto.PaymentRequest request
    ) {
        PaymentCommand.RequestPaymentCommand command = request.toCommand(loginId);
        PaymentInfo paymentInfo = paymentFacade.requestPayment(command);
        PaymentV1Dto.PaymentResponse response = PaymentV1Dto.PaymentResponse.from(paymentInfo);

        return ApiResponse.success(response);
    }

    @PostMapping("/callback")
    @Override
    public ApiResponse<Object> handlePaymentCallback(
            @RequestBody PaymentV1Dto.PaymentCallbackRequest request
    ) {
        log.info("결제 콜백 수신: transactionKey={}, orderKey={}, status={}",
                request.transactionKey(), request.orderId(), request.status());
        paymentFacade.handlePaymentCallback(request);
        return ApiResponse.success();
    }
}

