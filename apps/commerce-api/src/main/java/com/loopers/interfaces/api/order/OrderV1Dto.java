package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;
import org.springframework.data.domain.Page;

import java.util.List;

public class OrderV1Dto {

    public record OrderResponse(
            Long orderId,
            List<OrderItem> items,
            Integer totalPrice,
            PaymentMethod paymentMethod,
            OrderStatus status,
            String statusMessage,
            PaymentDetails paymentDetails
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
                    info.orderId(),
                    OrderItem.fromList(info.items()),
                    info.totalPrice(),
                    info.paymentMethod(),
                    info.status(),
                    generateStatusMessage(info),
                    PaymentDetails.from(info)
            );
        }

        private static String generateStatusMessage(OrderInfo info) {
            if (info.status() == null) {
                return "주문 상태 정보가 없습니다.";
            }
            return generateStatusMessage(info.status(), info.paymentMethod(), 
                    info.getPgTransactionKey(), info.getPgPaymentReason());
        }

        private static String generateStatusMessage(OrderStatus status, PaymentMethod paymentMethod,
                String pgTransactionKey, String pgPaymentReason) {
            return switch (status) {
                case PENDING -> {
                    if (paymentMethod == PaymentMethod.PG) {
                        if (pgTransactionKey == null) {
                            yield "PG 결제 요청 처리 중입니다. 잠시 후 다시 확인해주세요.";
                        } else {
                            yield "PG 결제 확인 중입니다. 잠시 후 다시 확인해주세요.";
                        }
                    } else {
                        yield "주문 처리 중입니다.";
                    }
                }
                case PAID -> {
                    if (paymentMethod == PaymentMethod.PG) {
                        yield "PG 결제가 완료되었습니다.";
                    } else {
                        yield "포인트 결제가 완료되었습니다.";
                    }
                }
                case FAILED -> {
                    if (pgPaymentReason != null && !pgPaymentReason.isBlank()) {
                        yield "결제 실패: " + pgPaymentReason;
                    } else {
                        yield "결제 처리 중 오류가 발생했습니다.";
                    }
                }
                case CANCELLED -> "주문이 취소되었습니다.";
            };
        }
    }

    public record PaymentDetails(
            String transactionKey,
            String failureReason,
            Integer retryCount,
            Boolean canRetry
    ) {
        public static PaymentDetails from(OrderInfo info) {
            if (info.paymentMethod() != PaymentMethod.PG) {
                return new PaymentDetails(null, null, null, null);
            }
            return new PaymentDetails(
                    info.getPgTransactionKey(),
                    info.getPgPaymentReason(),
                    info.retryCount(),
                    info.canRetry()
            );
        }
    }

    public record OrderItem(
            Long productId,
            String productName,
            Integer quantity,
            Integer totalPrice
    ) {
        public static OrderItem from(OrderItemInfo info) {
            return new OrderItem(
                    info.productId(),
                    info.productName(),
                    info.quantity(),
                    info.totalPrice()
            );
        }

        public static List<OrderItem> fromList(List<OrderItemInfo> infos) {
            return infos.stream()
                    .map(OrderItem::from)
                    .toList();
        }
    }

    public record OrderPageResponse(
            List<OrderResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {
        public static OrderPageResponse from(Page<OrderInfo> page) {
            return new OrderPageResponse(
                    page.map(OrderResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getNumberOfElements()
            );
        }
    }
}
