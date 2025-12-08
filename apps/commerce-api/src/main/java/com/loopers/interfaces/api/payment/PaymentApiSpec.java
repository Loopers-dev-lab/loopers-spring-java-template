package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment API", description = "결제 API입니다.")
public interface PaymentApiSpec {

  @Operation(
      summary = "결제 콜백 처리",
      description = "PG로부터 결제 결과를 전달받아 처리합니다."
  )
  ApiResponse<Void> handleCallback(
      @RequestBody(description = "결제 콜백 요청", required = true)
      PaymentCallbackRequest request
  );
}
