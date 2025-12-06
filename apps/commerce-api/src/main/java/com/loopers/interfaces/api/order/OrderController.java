package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApiSpec {

    private final OrderFacade orderFacade;
    private final PaymentFacade paymentFacade;

    @Override
    @PostMapping
    public ApiResponse<OrderDto.OrderResponse> createOrder(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody OrderDto.OrderCreateRequest request
    ) {
        List<OrderDto.OrderItemRequest> items = request.items().stream()
                .map(item -> new OrderDto.OrderItemRequest(item.productId(), item.quantity()))
                .toList();

        OrderDto.CardInfo cardInfo = request.cardInfo();
        String cardType = cardInfo != null ? cardInfo.cardType() : null;
        String cardNo = cardInfo != null ? cardInfo.cardNo() : null;

        OrderInfo info = orderFacade.createOrder(userId, items, cardType, cardNo);

        return ApiResponse.success(OrderDto.OrderResponse.from(info));
    }

    @Override
    @GetMapping
    public ApiResponse<OrderDto.OrderListResponse> getOrders(
            @RequestHeader("X-USER-ID") String userId
    ) {
        List<OrderInfo> orders = orderFacade.getOrders(userId);

        return ApiResponse.success(OrderDto.OrderListResponse.from(orders));
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto.OrderResponse> getOrder(
            @RequestHeader("X-USER-ID") String userId, @PathVariable Long orderId) {
        OrderInfo info = orderFacade.getOrder(userId, orderId);

        return ApiResponse.success(OrderDto.OrderResponse.from(info));
    }

    @GetMapping("/{orderId}/payment")
    public ApiResponse<OrderDto.PaymentStatusResponse> getPaymentStatus(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long orderId
    ) {
        orderFacade.getOrder(userId, orderId);

        Payment payment = paymentFacade.syncPaymentStatusWithPG(userId, orderId);

        OrderDto.PaymentStatusResponse response = new OrderDto.PaymentStatusResponse(
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getPaymentType().name(),
                payment.getAmount(),
                payment.getPgTransactionId(),
                payment.getFailureReason()
        );

        return ApiResponse.success(response);
    }
}
