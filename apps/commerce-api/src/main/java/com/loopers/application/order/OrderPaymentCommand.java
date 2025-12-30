package com.loopers.application.order;

public record OrderPaymentCommand(
        String cardType,
        String cardNo
) {
    @Override
    public String toString() {
        return "OrderPaymentCommand[cardType=" + cardType +
                ", cardNo=****" + (cardNo != null && cardNo.length() > 4 ? cardNo.substring(cardNo.length() - 4) : "") + "]";
    }
}
