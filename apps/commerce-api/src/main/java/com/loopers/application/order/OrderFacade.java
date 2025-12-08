package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.issuedcoupon.IssuedCouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final IssuedCouponService issuedCouponService;
    private final PaymentProcessor paymentProcessor;

    @Transactional
    public OrderInfo createOrder(OrderCommand command) {
        // 1. User 정보 조회
        User user = userService.getUser(command.userId());

        // 2. 쿠폰 처리
        Coupon coupon = null;
        IssuedCoupon issuedCoupon = null;
        if (command.couponId() != null) {
            coupon = couponService.getValidCoupon(command.couponId());
            issuedCoupon = issuedCouponService.getIssuedCouponByUser(user.getId(), command.couponId());
        }

        // 3. 상품 조회 (Pessimistic Lock)
        Map<Product, Integer> productQuantities = getProductQuantities(command);

        // 4. 주문 생성
        Order order = Order.createOrder(user, productQuantities, coupon, issuedCoupon);

        // 5. 재고 차감
        productQuantities.forEach(Product::decreaseStock);

        // 6. 주문 저장 (Payment가 Order를 참조하기 전에 먼저 저장)
        Order savedOrder = orderService.registerOrder(order);

        // 7. 결제 방식별 처리
        if (command.paymentType() == PaymentType.POINT) {
            // 포인트 결제는 동기 처리 (트랜잭션 분리 불필요)
            paymentProcessor.processPointPayment(user, savedOrder);
        } else if (command.paymentType() == PaymentType.CARD) {
            // 카드 결제는 별도 트랜잭션으로 분리 (PG 장애 시 Payment만 저장)
            paymentProcessor.processCardPayment(command, savedOrder.getId());
        }

        // 8. 쿠폰 사용 처리
        if (issuedCoupon != null) {
            issuedCoupon.useCoupon();
        }

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
}
