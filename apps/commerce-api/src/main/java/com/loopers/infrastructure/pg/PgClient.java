package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "pg-client",
    url = "${pg-client.base-url}",
    fallbackFactory = PgClientFallbackFactory.class
)
public interface PgClient {

  @PostMapping("/api/v1/payments")
  PgPaymentResponse requestPayment(
      @RequestHeader("X-USER-ID") String userId,
      @RequestBody PgPaymentRequest request
  );

  @GetMapping("/api/v1/payments/{transactionKey}")
  PgTransactionResponse getTransaction(
      @RequestHeader("X-USER-ID") String userId,
      @PathVariable("transactionKey") String transactionKey
  );
}
