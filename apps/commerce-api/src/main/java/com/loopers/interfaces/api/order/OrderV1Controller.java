package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto.OrderRequest;
import com.loopers.interfaces.api.order.OrderV1Dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

  private final OrderFacade orderFacade;


  @Override
  @PostMapping
  public ApiResponse<OrderResponse> createOrder(
      @RequestHeader("X-USER-ID") Long userId,
      @RequestBody OrderRequest request) {
    OrderInfo orderInfo = orderFacade.placeOrder(request.toCommand(userId));
    return ApiResponse.success(OrderResponse.from(orderInfo));
  }
}
