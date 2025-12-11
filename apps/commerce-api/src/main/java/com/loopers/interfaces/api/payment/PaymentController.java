package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController implements PaymentApiSpec {

    private final PaymentFacade paymentFacade;

    @PostMapping("/callback")
    @Override
    public ApiResponse<PaymentApiDto.PaymentResponse> callbackPayment(
            @RequestBody PaymentApiDto.PgCallbackRequest request
    ) {
        PaymentInfo paymentInfo = paymentFacade.callbackPayment(request);
        return ApiResponse.success(new PaymentApiDto.PaymentResponse(
                paymentInfo.orderId(),
                paymentInfo.transactionKey(),
                paymentInfo.status(),
                paymentInfo.reason()
        ));
    }
}

