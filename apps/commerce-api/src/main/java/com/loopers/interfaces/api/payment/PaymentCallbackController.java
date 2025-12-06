package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.infrastructure.pg.PgStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    public void handleCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        log.info("Payment callback received. transactionKey: {}, status: {}",
                request.getTransactionKey(), request.getStatus());

        PgStatus pgStatus = PgStatus.from(request.getStatus());

        if (pgStatus.isSuccess()) {
            paymentFacade.completePaymentByCallback(request.getTransactionKey());
        } else if (pgStatus.isFailed()) {
            paymentFacade.failPaymentByCallback(request.getTransactionKey(), request.getReason());
        }
    }
}
