package com.loopers.interfaces.api.order;

import com.loopers.application.order.CreateOrderCommand;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.application.order.OrderPaymentCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class OrderV1Dto {

    public record OrderCreateRequest(
            @Valid
            @NotEmpty(message = "상품 정보는 필수입니다.")
            List<OrderItemRequest> items,
            @Valid
            @NotNull(message = "결제 정보는 필수입니다.")
            PaymentRequest payment
    ) {
        public CreateOrderCommand toCommand(String userId) {
            List<OrderItemCommand> itemCommands = items.stream()
                    .map(OrderItemRequest::toCommand)
                    .toList();
            return new CreateOrderCommand(userId, itemCommands, payment.toCommand());
        }
    }

    public record OrderItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,
            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1개 이상이어야 합니다.")
            Long quantity
    ) {
        private OrderItemCommand toCommand() {
            return new OrderItemCommand(productId, quantity);
        }
    }

    public record PaymentRequest(
            @NotBlank(message = "카드사는 필수입니다.")
            String cardType,
            @NotBlank(message = "카드 번호는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$", message = "카드 번호 형식이 올바르지 않습니다.")
            String cardNo
    ) {
        private OrderPaymentCommand toCommand() {
            return new OrderPaymentCommand(cardType, cardNo);
        }
    }

    public record PaymentCallbackRequest(
            @NotNull(message = "상태는 필수입니다.")
            PgPaymentV1Dto.TransactionStatus status,
            String transactionKey,
            String reason
    ) {
    }

    public record OrderResponse(
            Long orderId,
            String userId,
            Long totalAmount,
            OrderStatus status,
            List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> responses = info.items().stream()
                    .map(OrderItemResponse::from)
                    .toList();
            return new OrderResponse(
                    info.orderId(),
                    info.userId(),
                    info.totalAmount(),
                    info.status(),
                    responses
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            Long quantity,
            Long price,
            Long amount
    ) {
        public static OrderItemResponse from(OrderItemInfo info) {
            return new OrderItemResponse(
                    info.productId(),
                    info.productName(),
                    info.quantity(),
                    info.price(),
                    info.amount()
            );
        }
    }
}
