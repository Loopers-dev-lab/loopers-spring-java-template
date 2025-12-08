package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCallbackFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentCallbackController implements PaymentApiSpec {

  private final PaymentCallbackFacade paymentCallbackFacade;

  @Override
  @PostMapping("/callback")
  public ApiResponse<Void> handleCallback(
      @RequestBody PaymentCallbackRequest request
  ) {
    paymentCallbackFacade.handleCallback(request);
    return ApiResponse.success(null);
  }
}
