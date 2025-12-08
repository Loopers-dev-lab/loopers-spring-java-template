package com.loopers.application.order;

import java.util.List;

import com.loopers.domain.payment.PaymentType;

import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 주문 생성 커맨드
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Builder
public record OrderCreateCommand(
        @NotBlank
        String username,

        @NotNull
        List<OrderItemCommand> orderItems,

        @NotNull
        PaymentType paymentType,

        @NotNull
        CardPaymentInfo cardInfo  // 카드 결제 시 사용
) {
    /**
     * 카드 결제 정보
     */
    public record CardPaymentInfo(
            @NotBlank
            String cardType,
            @NotBlank
            String cardNo,
            @NotBlank
            String callbackUrl
    ) {
    }
}
