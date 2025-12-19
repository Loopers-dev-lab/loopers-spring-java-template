package com.loopers.interfaces.api.order;

import com.loopers.application.order.CardInfo;
import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.order.Payment;
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
        OrderCreateCommand command = toCommand(userId, request);
        OrderInfo info = orderFacade.createOrder(command);
        return ApiResponse.success(OrderDto.OrderResponse.from(info));
    }

    private OrderCreateCommand toCommand(String userId, OrderDto.OrderCreateRequest request) {
        List<OrderDto.OrderItemRequest> items = request.items();
        CardInfo cardInfo = request.cardInfo();
        Long couponId = request.couponId();

        if (cardInfo != null && couponId != null) {
            return OrderCreateCommand.forCardPaymentWithCoupon(userId, items, cardInfo, couponId);
        } else if (cardInfo != null) {
            return OrderCreateCommand.forCardPayment(userId, items, cardInfo);
        } else if (couponId != null) {
            return OrderCreateCommand.forPointPaymentWithCoupon(userId, items, couponId);
        } else {
            return OrderCreateCommand.forPointPayment(userId, items);
        }
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
        OrderInfo orderInfo = orderFacade.getOrder(userId, orderId);

        if (orderInfo.paymentAmount() == 0) {
            return ApiResponse.success(new OrderDto.PaymentStatusResponse(
                    orderId,
                    "NO_PAYMENT",
                    null,
                    0L,
                    null,
                    null
            ));
        }

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
