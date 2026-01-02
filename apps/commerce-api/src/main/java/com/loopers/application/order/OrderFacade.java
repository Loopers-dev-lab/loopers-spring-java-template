package com.loopers.application.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.payment.PaymentProcessor;
import com.loopers.domain.activity.event.UserActivityEvent;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.issuedcoupon.IssuedCouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.outbox.OutboxEventService;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.kafka.AggregateTypes;
import com.loopers.kafka.KafkaTopics;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static com.loopers.kafka.KafkaTopics.Order.*;
import static com.loopers.kafka.KafkaTopics.UserActivity;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final IssuedCouponService issuedCouponService;

    private final PaymentProcessor paymentProcessor;

    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderInfo createOrder(OrderCommand command) {
        // 1. User 정보 조회
        User user = userService.getUser(command.userId());

        // 2. 쿠폰 처리
        Coupon coupon = null;
        IssuedCoupon issuedCoupon = null;
        if (command.couponId() != null) {
            // 1. 쿠폰 유효성 검증 (실패 시 예외)
            coupon = couponService.getValidCoupon(command.couponId());
            issuedCoupon = issuedCouponService
                    .getIssuedCouponByUser(user.getId(), command.couponId());

            // 2. 사전 검증을 통해 쿠폰 사용 가능 여부 검증
            issuedCoupon.validateCanUseCoupon();
        }

        // 3. 상품 조회 (Pessimistic Lock)
        Map<Product, Integer> productQuantities = getProductQuantities(command);

        // 4. 주문 생성
        Order order = Order.createOrder(user, productQuantities, coupon, issuedCoupon);

        // 5. 재고 차감
        productQuantities.forEach(Product::decreaseStock);

        // 6. 주문 저장 (Payment가 Order를 참조하기 전에 먼저 저장)
        Order savedOrder = orderService.registerOrder(order);

        // 7. 결제 처리(Command)
        if (command.paymentType() == PaymentType.POINT) {
            paymentProcessor.processPointPayment(
                    user.getId(),
                    savedOrder.getId()
            );
        } else if (command.paymentType() == PaymentType.CARD) {
            paymentProcessor.processCardPayment(
                    savedOrder.getId(),
                    command.cardType(),
                    command.cardNo()
            );
        }

        // 8. 쿠폰 사용 처리 (이벤트 발행 실패 시에도 주문은 성공 처리)
        if (issuedCoupon != null) {
            publishCouponUsedEvent(user, coupon, savedOrder, order);
        }

        // 9. 주문 생성 완료 이벤트 발행 (이벤트 발행 실패 시에도 주문은 성공 처리)
        publishOrderCreatedEvent(savedOrder, user, command);

        // 10. 사용자 행동 추적 이벤트 발행 (이벤트 발행 실패 시에도 주문은 성공 처리)
        publishUserActivityEvent(user, savedOrder);

        return OrderInfo.from(savedOrder);
    }

    /**
     * 상품 조회 및 검증
     */
    private Map<Product, Integer> getProductQuantities(OrderCommand command) {
        Map<Product, Integer> productQuantities = new HashMap<>();
        for (OrderCommand.OrderItemCommand item : command.items()) {
            Product product = productService.getProductWithLock(item.productId());

            if (productQuantities.containsKey(product)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "동일 상품이 중복으로 요청되었습니다");
            }

            productQuantities.put(product, item.quantity());
        }
        return productQuantities;
    }

    /**
     * 쿠폰 사용 이벤트 발행
     * 실패 시에도 주문 트랜잭션에 영향을 주지 않음
     */
    private void publishCouponUsedEvent(User user, Coupon coupon, Order savedOrder, Order order) {
        try {
            CouponUsedEvent couponUsedEvent = CouponUsedEvent.of(
                    user.getId(),
                    coupon.getId(),
                    savedOrder.getId(),
                    order.getTotalPrice()
            );

            String couponUsePayload = objectMapper.writeValueAsString(couponUsedEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.COUPON,
                    savedOrder.getId().toString(),
                    KafkaTopics.Coupon.COUPON_USED,
                    couponUsePayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 남기고 주문은 성공 처리
            log.error("CouponUsedEvent 직렬화 실패 - 주문은 성공 처리됨. orderId: {}, couponId: {}",
                    savedOrder.getId(), coupon.getId(), e);
        }
    }

    /**
     * 주문 생성 이벤트 발행
     * 실패 시에도 주문 트랜잭션에 영향을 주지 않음
     */
    private void publishOrderCreatedEvent(Order savedOrder, User user, OrderCommand command) {
        try {
            // 저장된 Order의 OrderItem에서 실제 가격 정보 포함
            var orderItems = savedOrder.getOrderItems().stream()
                    .map(item -> new OrderCreatedEvent.OrderItem(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            item.getPrice().getAmount()  // 상품 단가
                    ))
                    .toList();

            OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.of(
                    savedOrder.getId(),
                    user.getId(),
                    savedOrder.getTotalPrice().getAmount(),
                    command.paymentType(),
                    orderItems
            );

            String orderCreatePayload = objectMapper.writeValueAsString(orderCreatedEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.ORDER,
                    savedOrder.getId().toString(),
                    ORDER_CREATED,
                    orderCreatePayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 남기고 주문은 성공 처리
            log.error("OrderCreatedEvent 직렬화 실패 - 주문은 성공 처리됨. orderId: {}",
                    savedOrder.getId(), e);
        }
    }

    /**
     * 사용자 활동 이벤트 발행
     * 실패 시에도 주문 트랜잭션에 영향을 주지 않음
     */
    private void publishUserActivityEvent(User user, Order savedOrder) {
        try {
            UserActivityEvent userActivityEvent = UserActivityEvent.of(
                    user.getUserId(),
                    "ORDER_CREATED",
                    "ORDER",
                    savedOrder.getId()
            );

            String userActivityPayload = objectMapper.writeValueAsString(userActivityEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.ACTIVITY,
                    savedOrder.getId().toString(),
                    UserActivity.USER_ACTIVITY,
                    userActivityPayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 남기고 주문은 성공 처리
            log.error("UserActivityEvent 직렬화 실패 - 주문은 성공 처리됨. orderId: {}, userId: {}",
                    savedOrder.getId(), user.getId(), e);
        }
    }
}
