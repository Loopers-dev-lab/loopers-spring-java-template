package com.loopers.application.order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentType;

import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author hyunjikoh
 * @since 2025. 12. 8.
 */
public class OrderFacadeDtos {
    /**
     * 주문 정보 응답 DTO
     *
     * @author hyunjikoh
     * @since 2025. 11. 14.
     */
    public record OrderInfo(
            Long id,
            Long userId,
            Long orderNumber,
            BigDecimal originalTotalAmount,
            BigDecimal discountAmount,
            BigDecimal finalTotalAmount,
            OrderStatus status,
            List<OrderItemInfo> orderItems,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {

        /**
         * OrderEntity로부터 OrderInfo를 생성합니다.
         *
         * @param order      주문 엔티티
         * @param orderItems 주문 항목 엔티티 목록
         * @return OrderInfo
         */
        public static OrderInfo from(OrderEntity order, List<OrderItemEntity> orderItems) {
            return new OrderInfo(
                    order.getId(),
                    order.getUserId(),
                    order.getOrderNumber(),
                    order.getOriginalTotalAmount(),
                    order.getDiscountAmount(),
                    order.getFinalTotalAmount(),
                    order.getStatus(),
                    orderItems.stream()
                            .map(OrderItemInfo::from)
                            .toList(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }

    /**
     * 주문 항목 커맨드
     *
     * @author hyunjikoh
     * @since 2025. 11. 14.
     */
    @Builder
    public record OrderItemCommand(
            Long productId,
            Integer quantity,
            Long couponId
    ) {
    }

    /**
     * 주문 항목 정보 응답 DTO
     *
     * @author hyunjikoh
     * @since 2025. 11. 14.
     */
    public record OrderItemInfo(
            Long id,
            Long orderId,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal totalPrice,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {

        /**
         * OrderItemEntity로부터 OrderItemInfo를 생성합니다.
         *
         * @param orderItem 주문 항목 엔티티
         * @return OrderItemInfo
         */
        public static OrderItemInfo from(OrderItemEntity orderItem) {
            return new OrderItemInfo(
                    orderItem.getId(),
                    orderItem.getOrderId(),
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getDiscountAmount(),
                    orderItem.getTotalPrice(),
                    orderItem.getCreatedAt(),
                    orderItem.getUpdatedAt()
            );
        }
    }

    /**
     * 주문 목록 조회용 요약 정보 응답 DTO
     * 주문 항목 정보를 포함하지 않아 가볍게 조회 가능
     *
     * @author hyunjikoh
     * @since 2025. 11. 14.
     */
    public record OrderSummary(
            Long id,
            Long userId,
            Long orderNumber,
            BigDecimal originalTotalAmount,
            BigDecimal discountAmount,
            BigDecimal finalTotalAmount,
            OrderStatus status,
            int itemCount,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {

        /**
         * OrderEntity로부터 OrderSummary를 생성합니다.
         *
         * @param order     주문 엔티티
         * @param itemCount 주문 항목 개수
         * @return OrderSummary
         */
        public static OrderSummary from(OrderEntity order, int itemCount) {
            return new OrderSummary(
                    order.getId(),
                    order.getUserId(),
                    order.getOrderNumber(),
                    order.getOriginalTotalAmount(),
                    order.getDiscountAmount(),
                    order.getFinalTotalAmount(),
                    order.getStatus(),
                    itemCount,
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }

    /**
     * 주문 생성 커맨드
     *
     * @author hyunjikoh
     * @since 2025. 11. 14.
     */
    @Builder
    @Deprecated
    public record OrderCreateCommand(
            @NotBlank
            String username,

            @NotNull
            List<OrderItemCommand> orderItems,

            PaymentType paymentType,

            OrderFacadeDtos.OrderCreateCommand.CardPaymentInfo cardInfo  // 카드 결제 시 사용
    ) {
        /**
         * 카드 결제 정보
         */
        public record CardPaymentInfo(
                @NotBlank
                String cardType,
                @NotBlank
                String cardNo,
                @NotBlank
                String callbackUrl
        ) {
        }
    }

    /**
     * 포인트 결제용 주문 생성 커맨드
     */
    @Builder
    public record PointOrderCreateCommand(
            @NotBlank
            String username,

            @NotNull
            List<OrderItemCommand> orderItems
    ) {
    }

    /**
     * 카드 결제용 주문 생성 커맨드
     */
    @Builder
    public record CardOrderCreateCommand(
            @NotBlank
            String username,

            @NotNull
            List<OrderItemCommand> orderItems,

            @NotNull
            @jakarta.validation.Valid
            CardPaymentInfo cardInfo
    ) {
        /**
         * 카드 결제 정보
         */
        public record CardPaymentInfo(
                @NotBlank
                String cardType,
                @NotBlank
                String cardNo,
                @NotBlank
                String callbackUrl
        ) {
        }
    }

    /**
     * 주문 + 결제 정보 래퍼
     */
    public record OrderWithPaymentInfo(
            OrderFacadeDtos.OrderInfo order,
            PaymentInfo payment
    ) {
    }
}
