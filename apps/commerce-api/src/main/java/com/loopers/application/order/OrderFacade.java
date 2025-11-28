package com.loopers.application.order;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private final CouponService couponService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;
    private final SupplyService supplyService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public OrderInfo getOrderInfo(String userId, Long orderId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Order order = orderService.getOrderByIdAndUserId(orderId, user.getId());

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrderList(String userId, Pageable pageable) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Page<Order> orders = orderService.getOrdersByUserId(user.getId(), pageable);
        return orders.map(OrderInfo::from);
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderRequest request) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        request.validate();

        Map<Long, Integer> productIdQuantityMap = request.toItemQuantityMap();

        productIdQuantityMap.forEach(supplyService::checkAndDecreaseStock);

        List<Product> products = productService.getProductsByIds(productIdQuantityMap.keySet());
        Map<Product, Integer> productQuantityMap = products.stream().collect(Collectors.toMap(
                Function.identity(),
                product -> productIdQuantityMap.get(product.getId())
        ));

        Price originalPrice = productQuantityMap.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(entry.getValue()))
                .reduce(new Price(0), Price::add);

        DiscountResult discountResult = getDiscountResult(request.couponId(), user, originalPrice);

        pointService.checkAndDeductPoint(user.getId(), discountResult.finalPrice());
        Order order = orderService.createOrder(productQuantityMap, user.getId(), discountResult);

        return OrderInfo.from(order);
    }

    private DiscountResult getDiscountResult(Long couponId, User user, Price originalPrice) {
        if (couponId != null) {
            return couponService.applyCoupon(couponId, user.getId(), originalPrice);
        }
        return new DiscountResult(originalPrice);
    }

}
