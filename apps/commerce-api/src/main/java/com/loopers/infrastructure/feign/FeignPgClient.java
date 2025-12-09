package com.loopers.infrastructure.feign;

import com.loopers.application.payment.PgPayRequest;
import com.loopers.application.payment.PgPayResponse;
import com.loopers.application.payment.PgPaymentInfoResponse;
import com.loopers.application.payment.PgPaymentListResponse;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "pgClient",
    url = "http://localhost:8082"
)
public interface FeignPgClient {

  @Retry(name = "pgRetry")
  @PostMapping("/api/v1/payments")
  ApiResponse<PgPayResponse> requestPayment(
      @RequestHeader("X-USER-ID") String userId,
      @RequestBody PgPayRequest request
  );

  @GetMapping("/api/v1/payments/{transactionKey}")
  ApiResponse<PgPaymentInfoResponse> getPaymentInfo(
      @RequestHeader("X-USER-ID") String userId,
      @PathVariable String transactionKey
  );

  @GetMapping("/api/v1/payments")
  ApiResponse<PgPaymentListResponse> getPaymentsByOrder(
      @RequestHeader("X-USER-ID") String userId,
      @RequestParam String orderId
  );
}

