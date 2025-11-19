package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;
    private final CouponService couponService;

    @Transactional
    public OrderInfo placeOrder(OrderPlaceCommand command) {
        User user = userService.getUserByUserId(command.userId());

        List<Long> productIds = command.items().stream()
                .map(OrderPlaceCommand.OrderItemCommand::productId)
                .sorted()
                .toList();

        List<Product> products = productService.getProductsByIdsWithPessimisticLock(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        validateAndDecreaseStock(command.items(), productMap);

        Order order = Order.create(user);

        for (OrderPlaceCommand.OrderItemCommand item : command.items()) {
            Product product = productMap.get(item.productId());
            order.addOrderItem(product, item.quantity());
        }

        Long totalAmount = order.getTotalAmountValue();
        Long discountAmount = 0L;
        Long couponId = command.couponId();

        if (couponId != null) {
            try {
                Coupon coupon = couponService.getCouponWithOptimisticLock(couponId);
                couponService.validateCouponUsable(coupon, user);

                discountAmount = coupon.calculateDiscount(totalAmount);
                coupon.use();
                couponService.save(coupon);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new CoreException(ErrorType.CONFLICT,
                        "쿠폰이 이미 사용 중입니다. 잠시 후 다시 시도해주세요.");
            }
        }
        long finalAmount = totalAmount - discountAmount;
        pointService.usePointWithLock(user.getUserIdValue(), finalAmount);

        order.completePayment();
        Order savedOrder = orderService.save(order);

        return OrderInfo.from(savedOrder);
    }

    private void validateAndDecreaseStock(List<OrderPlaceCommand.OrderItemCommand> items, Map<Long, Product> productMap) {
        for (OrderPlaceCommand.OrderItemCommand item : items) {
            Product product = productMap.get(item.productId());

            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
            }

            if (!product.isStockAvailable(item.quantity())) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                        String.format("상품 '%s'의 재고가 부족합니다.", product.getName()));
            }

            product.decreaseStock(item.quantity());
        }
    }

    public List<OrderInfo> getMyOrders(String userId) {
        User user = userService.getUserByUserId(userId);
        List<Order> orders = orderService.getOrdersByUser(user);

        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    public OrderInfo getOrderDetail(Long orderId, String userId) {
        User user = userService.getUserByUserId(userId);
        Order order = orderService.getOrderByIdAndUser(orderId, user);

        return OrderInfo.from(order);
    }
}
