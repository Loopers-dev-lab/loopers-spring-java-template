package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderListDto;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.quantity.Quantity;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderCreateRequest;
import com.loopers.interfaces.api.order.OrderDto.OrderDetailResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderListResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApiSpec {

  private final OrderFacade orderFacade;

  @Override
  @PostMapping
  public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @Valid @RequestBody OrderCreateRequest request
  ) {
    List<OrderItemCommand> commands = request.items().stream()
        .map(item -> OrderItemCommand.of(item.productId(), Quantity.of(item.quantity())))
        .toList();

    Order order = orderFacade.createOrder(userId, commands);
    OrderDetailResponse response = OrderDetailResponse.from(order);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response));
  }

  @Override
  @GetMapping
  public ApiResponse<OrderListResponse> retrieveOrders(
      @RequestHeader("X-USER-ID") Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);

    Page<OrderListDto> orderPage = orderFacade.retrieveOrders(userId, pageable);
    OrderListResponse response = OrderListResponse.from(orderPage);

    return ApiResponse.success(response);
  }

  @Override
  @GetMapping("/{orderId}")
  public ApiResponse<OrderDetailResponse> retrieveOrderDetail(
      @RequestHeader("X-USER-ID") Long userId,
      @PathVariable Long orderId
  ) {
    Order order = orderFacade.retrieveOrderDetail(userId, orderId);
    OrderDetailResponse response = OrderDetailResponse.from(order);

    return ApiResponse.success(response);
  }
}
