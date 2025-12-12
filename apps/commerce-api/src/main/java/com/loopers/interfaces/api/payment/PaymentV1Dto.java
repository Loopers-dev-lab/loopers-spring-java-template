package com.loopers.interfaces.api.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentV1Dto {

    public record PaymentRequest(
            @NotNull(message = "주문키는 필수입니다.")
            String orderKey,
            @NotBlank(message = "카드 타입은 필수입니다.")
            String cardType,
            @NotBlank(message = "카드 번호는 필수입니다.")
            String cardNumber
    ) {
        public PaymentCommand.RequestPaymentCommand toCommand(String loginId) {
            return new PaymentCommand.RequestPaymentCommand(
                    loginId,
                    orderKey,
                    cardType,
                    cardNumber
            );
        }
    }

    public record PaymentResponse(
            Long paymentId,
            String orderKey,
            String transactionKey,
            String status,
            Integer amount
    ) {
        public static PaymentResponse from(PaymentInfo paymentInfo) {
            return new PaymentResponse(
                    paymentInfo.paymentId(),
                    paymentInfo.orderKey(),
                    paymentInfo.transactionKey(),
                    paymentInfo.status(),
                    paymentInfo.amount()
            );
        }
    }

    public record PaymentCallbackRequest(
            @JsonProperty("transactionKey") String transactionKey,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("status") String status,
            @JsonProperty("reason") String reason
    ) {
    }
}



