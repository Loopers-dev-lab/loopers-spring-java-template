package com.loopers.domain.payment;

public record PaymentApproveInfo(
        String orderId,
        CardType cardType,
        String cardNo,
        Long amount,
        String callbackUrl
) {
    public static PaymentApproveInfo from(final String prefixPaymentIdKey, final String prefixCallBackUrl,
                                          final Payment payment) {
        return new PaymentApproveInfo(
                prefixPaymentIdKey + payment.getId(),
                payment.getCardType(),
                payment.getCardNo(),
                payment.getAmount(),
                prefixCallBackUrl

        );
    }
}
