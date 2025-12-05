package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PgClientFallbackFactory implements FallbackFactory<PgClient> {

  @Override
  public PgClient create(Throwable cause) {
    return new PgClient() {
      @Override
      public PgPaymentResponse requestPayment(String userId, PgPaymentRequest request) {
        throw new PgRequestFailedException("PG 결제 요청 실패: " + cause.getMessage(), cause);
      }

      @Override
      public PgTransactionResponse getTransaction(String userId, String transactionKey) {
        throw new PgRequestFailedException("PG 조회 실패: " + cause.getMessage(), cause);
      }
    };
  }
}
