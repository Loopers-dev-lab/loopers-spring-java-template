package com.loopers.application.order;

import com.loopers.application.order.OrderCommand.PaymentMethod;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Delivery;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final PointService pointService;
    private final PaymentService paymentService;

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100),
            recover = "recoverCreateOrder"
    )
    @Transactional
    public OrderInfo createOrder(OrderCommand.CreateOrderCommand createOrderCommand) {
        User user = userService.findUserByLoginId(createOrderCommand.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Delivery delivery = OrderCommand.DeliveryCommand.toDelivery(createOrderCommand.delivery());
        Order order = orderService.createOrder(user.getId(), delivery);

        int originalTotalPrice = 0;
        for (OrderCommand.OrderItemCommand orderItemCommand : createOrderCommand.orderItems()) {
            Product product = productService.getProductById(orderItemCommand.productId());

            orderService.createOrderItem(order, product, orderItemCommand.quantity());
            originalTotalPrice += orderService.addTotalPrice(order, product.getPrice().getPrice(), orderItemCommand.quantity());

            productService.reduceStock(product, orderItemCommand.quantity());
        }

        Long couponId = createOrderCommand.couponId();
        int discountPrice = 0;
        if (couponId != null) {
            Coupon coupon = couponService.getCouponByIdAndUserId(couponId, user.getId());
            discountPrice = couponService.calculateDiscountPrice(coupon, originalTotalPrice);
            orderService.applyCoupon(order, coupon, discountPrice);
            couponService.usedCoupon(coupon);
        }

        int finalAmount = originalTotalPrice - discountPrice;

        PaymentMethod paymentMethod = createOrderCommand.paymentMethod();
        if (paymentMethod == PaymentMethod.POINT) {
            pointService.deductPoint(user.getId(), finalAmount);
        } else if (paymentMethod == PaymentMethod.CARD) {
            paymentService.createPayment(finalAmount, order.getOrderKey());
        } else {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 결제 수단입니다.");
        }

        orderService.applyPrice(order, originalTotalPrice, discountPrice);
        orderService.saveOrder(order);

        return OrderInfo.from(order, order.getOrderItems(), delivery, order.getOrderKey());
    }

    @Recover
    public OrderInfo recoverCreateOrder(OptimisticLockingFailureException e, OrderCommand.CreateOrderCommand createOrderCommand) {
        throw new CoreException(ErrorType.CONFLICT, "주문 생성 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
    }

    @Transactional
    public List<OrderInfo> getOrdersInfo(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<Order> orders = orderService.findOrdersByUserId(user.getId());

        return orders.stream()
                .map(order -> OrderInfo.from(order, order.getOrderItems(), order.getDelivery(), null))
                .toList();
    }

    @Transactional
    public OrderInfo getOrderInfo(String loginId, Long orderId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Order order = orderService.findOrderByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        return OrderInfo.from(order, order.getOrderItems(), order.getDelivery(), null);
    }
}
