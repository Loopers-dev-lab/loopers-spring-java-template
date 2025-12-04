package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.issuedcoupon.IssuedCouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final IssuedCouponService issuedCouponService;

    @Transactional
    public OrderInfo createOrder(String userId,
                                 List<OrderV1Dto.OrderRequest.OrderItemRequest> items,
                                 Long couponId) {
        // 1. User 정보 조회
        User user = userService.getUser(userId);

        // 2. 쿠폰 처리 (도메인 서비스에서 검증 포함)
        Coupon coupon = null;
        IssuedCoupon issuedCoupon = null;

        if (couponId != null) {
            // 쿠폰 유효성 검증 (도메인 서비스에서 처리)
            coupon = couponService.getValidCoupon(couponId);

            // 사용자가 해당 쿠폰을 발급받았는지 확인 (도메인 서비스에서 검증)
            issuedCoupon = issuedCouponService.getIssuedCouponByUser(user.getId(), couponId);
        }

        // 3. Product 조회 및 Map 생성
        Map<Product, Integer> productQuantities = new HashMap<>();
        for (OrderV1Dto.OrderRequest.OrderItemRequest item : items) {
            Product product = productService.getProductWithLock(item.productId());

            if (productQuantities.containsKey(product)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "동일 상품이 중복으로 요청되었습니다");
            }

            productQuantities.put(product, item.quantity());
        }

        // 4. Order 생성 (쿠폰 정보 전달하여 할인 적용)
        Order order = Order.createOrder(user, productQuantities, coupon);

        // 5. 재고 차감
        productQuantities.forEach((product, quantity) ->
            product.decreaseStock(quantity)
        );

        // 6. 포인트 차감 (할인이 적용된 총액)
        user.usePoint(order.getTotalPrice());

        // 7. 쿠폰 사용 처리
        if (issuedCoupon != null) {
            issuedCoupon.useCoupon();
        }

        // 8. Order 저장
        Order savedOrder = orderService.registerOrder(order);

        return OrderInfo.from(savedOrder);
    }
}
