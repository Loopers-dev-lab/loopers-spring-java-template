package com.loopers.interfaces.api.batch;

import com.loopers.application.payment.PaymentSyncBatchService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/batch/payments")
@RequiredArgsConstructor
public class PaymentBatchV1Controller {

  private final PaymentSyncBatchService batchService;

  @PostMapping("/sync-pending")
  public ApiResponse<String> syncPendingPayments() {
    batchService.syncPendingPayments();
    return ApiResponse.success("결제 상태 동기화 배치가 실행되었습니다.");
  }

  @PostMapping("/sync-failed")
  public ApiResponse<String> syncFailedPayments() {
    batchService.syncFailedPayments();
    return ApiResponse.success("실패 결제 재확인 배치가 실행되었습니다.");
  }
}