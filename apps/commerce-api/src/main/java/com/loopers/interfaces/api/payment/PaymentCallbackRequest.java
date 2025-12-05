package com.loopers.interfaces.api.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * PG 결제 콜백 요청 DTO
 * PG-Simulator의 TransactionInfo와 동일한 구조
 *
 * @param transactionKey 트랜잭션 KEY
 * @param orderId 주문 ID
 * @param cardType 카드 종류
 * @param cardNo 카드 번호
 * @param amount 금액
 * @param status 처리 상태 (SUCCESS, FAILED)
 * @param reason 처리 사유 (실패 시)
 */
public record PaymentCallbackRequest(
    @NotBlank(message = "transactionKey는 필수입니다.") String transactionKey,
    @NotBlank(message = "orderId는 필수입니다.") String orderId,
    String cardType,
    String cardNo,
    @NotNull(message = "amount는 필수입니다.") Long amount,
    @NotBlank(message = "status는 필수입니다.") String status,
    String reason
) {

  public boolean isSuccess() {
    return "SUCCESS".equals(status);
  }

  public boolean isFailed() {
    return "FAILED".equals(status);
  }

}
