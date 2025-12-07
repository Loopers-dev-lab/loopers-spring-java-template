package com.loopers.interfaces.api.payment;

import com.loopers.application.order.OrderFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제", description = "결제 콜백 API")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCallbackController {

    private final OrderFacade orderFacade;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @PostMapping("/callback")
    public void handleCallback(@RequestBody PaymentV1Dto.PgPaymentResponse response) {
        try {
            if (response.result() == PaymentV1Dto.pgPaymentStatus.SUCCESS) {
                orderFacade.onPaymentSuccess(response.orderId());
            } else {
                orderFacade.onPaymentFailure(response.orderId());
            }

        } catch (Exception e) {
            log.warn("결제 서버 오류, {}", e.getMessage());
            orderFacade.onPaymentFailure(response.orderId());
        }
    }
}
