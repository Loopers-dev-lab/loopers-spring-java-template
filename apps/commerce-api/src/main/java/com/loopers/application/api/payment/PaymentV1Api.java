package com.loopers.application.api.payment;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.service.payment.PaymentService;
import com.loopers.core.service.payment.PgCallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.loopers.application.api.payment.PaymentV1Dto.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Api implements PaymentsV1ApiSpec {

    private final PaymentService paymentService;
    private final PgCallbackService pgCallbackService;

    @Override
    @PostMapping("/orders/{orderId}")
    public ApiResponse<PaymentResponse> order(
            @RequestHeader(name = "X-USER-ID") String userIdentifier,
            @PathVariable String orderId,
            @RequestBody @Valid PaymentRequest paymentRequest
    ) {
        Payment payment = paymentService.pay(paymentRequest.toCommand(orderId, userIdentifier));

        return ApiResponse.success(PaymentResponse.from(payment));
    }

    @Override
    @PostMapping("/callback")
    public ApiResponse<?> callback(@RequestBody @Valid PgCallbackRequest request) {
        pgCallbackService.callback(request.toCommand());
        return ApiResponse.success();
    }
}
