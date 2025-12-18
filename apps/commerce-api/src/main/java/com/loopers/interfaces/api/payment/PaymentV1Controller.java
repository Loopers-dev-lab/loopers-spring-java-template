package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@RestController
public class PaymentV1Controller implements PaymentV1ApiSpec {
    private final PaymentFacade paymentFacade;

    @Override
    @PostMapping("/callback")
    public ApiResponse<PointV1Dto.PointResponse> callback(final PaymentV1Dto.PaymentCallbackRequest request) {
        paymentFacade.callback(request.toTransactionInfo());
        return null;
    }
}
