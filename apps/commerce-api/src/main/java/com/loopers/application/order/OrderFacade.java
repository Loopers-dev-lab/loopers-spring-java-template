package com.loopers.application.order;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final StockService stockService;
    private final PointService pointService;
    private final CouponService couponService;

    /**
     * 주문 생성
     */
    @Transactional
    public OrderInfo createOrder(String loginId, OrderDto.CreateOrderRequest request) {
        List<OrderDto.OrderItemRequest> orderItemRequests = request.items();
        // 1. User 객체 조회
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND, 
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        // 2. Product 조회 및 재고 차감
        Map<Long, Product> productMap = new HashMap<>();
        orderItemRequests.stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId))  // productId로 정렬 (데드락 방지)
                .forEach(itemRequest -> {
                    // 2-1. Product 객체 조회
                    Product product = productService.findById(itemRequest.productId())
                            .orElseThrow(() -> new CoreException(
                                ErrorType.NOT_FOUND, 
                                "[productId = " + itemRequest.productId() + "] Product를 찾을 수 없습니다."
                    ));
                    productMap.put(itemRequest.productId(), product);

                    // 2-2. Stock(재고) 차감, 만약 재고가 부족하면 예외 발생
                    stockService.decreaseQuantity(itemRequest.productId(), Long.valueOf(itemRequest.quantity()));
                });

        // 3. 주문 생성
        Order order = Order.builder()
                .discountAmount(BigDecimal.ZERO)  // 초기 할인 금액은 0 (쿠폰 적용 전)
                .shippingFee(BigDecimal.ZERO)     // 배송비는 기본값 0 (필요시 비즈니스 로직으로 계산)
                .user(user)
                .build();
        
        // 3-1. OrderItem 리스트 생성 및 적용 (Order를 통해서만 생성)
        orderItemRequests.forEach(itemRequest -> {
            Product product = productMap.get(itemRequest.productId());
            order.addOrderItem(product, itemRequest.quantity());
        });

        // 3-2. 주문 먼저 저장 (쿠폰 적용을 위해 order.getId()가 필요)
        Order savedOrder = orderService.saveOrder(order)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Order 저장에 실패했습니다."
                ));

        // 4. 쿠폰 적용 (포인트 차감 전에 할인 적용)
        if (request.couponIds() != null && !request.couponIds().isEmpty()) {
                for(Long couponId : request.couponIds()) {
                        couponService.useCoupon(savedOrder, couponId);
                }
        }

        // 5. 포인트 차감 (쿠폰 할인 적용 후 최종 금액으로)
        pointService.deduct(loginId, savedOrder.getFinalAmount());
        
        // 6. 주문 상태 변경
        savedOrder.confirm();

        // 7. 주문 상태 변경 후 다시 저장
        savedOrder = orderService.saveOrder(savedOrder)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Order 저장에 실패했습니다."
                ));

        // 주문 정보 반환
        return OrderInfo.from(savedOrder);
    }

    /**
     * 단일 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderInfo getOrder(Long orderId) {
        Order order = orderService.findOrderById(orderId);
        return OrderInfo.from(order);
    }

    /**
     * 유저의 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        List<Order> orders = orderService.findOrdersByUser(user);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }
}

