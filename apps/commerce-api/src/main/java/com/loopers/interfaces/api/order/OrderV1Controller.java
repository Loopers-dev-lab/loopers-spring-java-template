package com.loopers.interfaces.api.order;

import com.loopers.application.order.CreateOrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

  private final OrderFacade orderFacade;

  @GetMapping("")
  @Override
  public ApiResponse<Page<Order>> getOrderList(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @RequestParam(name = "sortType", defaultValue = "latest") String sortType,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    Page<Order> info = orderFacade.getOrderList(userId, sortType, page, size);
    return ApiResponse.success(info);
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  public ApiResponse<OrderCreateV1Dto.OrderResponse> createOrder(@RequestHeader(value = "X-USER-ID", required = false) Long userId
      , @RequestBody OrderCreateV1Dto.OrderRequest request
  ) {
    CreateOrderCommand command = CreateOrderCommand.from(userId, request);
    OrderInfo info = orderFacade.createOrder(command);
    OrderCreateV1Dto.OrderResponse response = OrderCreateV1Dto.OrderResponse.from(info);
    return ApiResponse.success(response);
  }

  @GetMapping("/{orderId}")
  @Override
  public ApiResponse<OrderCreateV1Dto.OrderResponse> getOrderDetail(@RequestHeader(value = "X-USER-ID", required = false) Long userId
      , @PathVariable(value = "orderId") Long orderId
  ) {
    OrderInfo info = orderFacade.getOrderDetail(orderId);
    OrderCreateV1Dto.OrderResponse response = OrderCreateV1Dto.OrderResponse.from(info);
    return ApiResponse.success(response);
  }
}
