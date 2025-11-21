package com.loopers.application.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.dto.OrderCreationResult;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 주문 Facade
 *
 * <p>주문 생성, 확정, 취소 등의 유스케이스를 조정합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;
    private final CouponService couponService;

    /**
     * 주문 생성
     *
     * <p>여러 도메인 서비스를 조정하여 주문 생성 유스케이스를 완성합니다.</p>
     *
     * @param command 주문 생성 명령
     * @return 생성된 주문 정보
     * @throws IllegalArgumentException 재고 부족 또는 주문 불가능한 경우
     */
    @Transactional
    public OrderInfo createOrder(OrderCreateCommand command) {
        // 1. 주문자 정보 조회 (락 적용)
        UserEntity user = userService.findByUsernameWithLock(command.username());

        // 2. 주문 항목을 상품 ID 기준으로 정렬 (교착 상태 방지)
        List<OrderItemCommand> sortedItems = command.orderItems().stream()
                .sorted(Comparator.comparing(OrderItemCommand::productId))
                .toList();

        // 3. 상품 검증 및 준비 (재고 확인, 락 적용)
        List<ProductEntity> orderableProducts = new ArrayList<>();
        List<CouponEntity> coupons = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (OrderItemCommand itemCommand : sortedItems) {
            // 상품 정보 조회 및 재고 잠금
            ProductEntity product = productService.getProductDetailLock(itemCommand.productId());

            // 재고 확인
            if (!product.canOrder(itemCommand.quantity())) {
                throw new IllegalArgumentException(
                        String.format("주문할 수 없는 상품입니다. 상품 ID: %d, 요청 수량: %d, 현재 재고: %d",
                                product.getId(), itemCommand.quantity(), product.getStockQuantity())
                );
            }

            // 쿠폰 검증 및 준비
            CouponEntity coupon = itemCommand.couponId() != null
                    ? couponService.getCouponByIdAndUserId(itemCommand.couponId(), user.getId())
                    : null;
            if (coupon != null && coupon.isUsed()) {
                throw new IllegalArgumentException("이미 사용된 쿠폰입니다.");
            }

            orderableProducts.add(product);
            coupons.add(coupon);
            quantities.add(itemCommand.quantity());
        }

        // 4. 도메인 서비스: 주문 및 주문 항목 생성 (도메인 로직)
        OrderCreationResult creationResult = orderService.createOrderWithItems(
                user.getId(),
                orderableProducts,
                coupons,
                quantities
        );

        // 5. 포인트 차감
        pointService.use(user, creationResult.order().getFinalTotalAmount());

        // 6. 쿠폰 사용 처리
        coupons.stream().filter(Objects::nonNull).forEach(couponService::consumeCoupon);

        IntStream.range(0, orderableProducts.size())
                .forEach(i -> productService.deductStock(orderableProducts.get(i), quantities.get(i)));

        // 8. 주문 정보 반환
        return OrderInfo.from(creationResult.order(), creationResult.orderItems());
    }

    /**
     * 주문 확정
     *
     * <p>주문을 확정합니다. (재고는 이미 주문 생성 시 차감되었음)</p>
     *
     * @param orderId  주문 ID
     * @param username 사용자명
     * @return 확정된 주문 정보
     */
    @Transactional
    public OrderInfo confirmOrder(Long orderId, String username) {
        UserEntity user = userService.getUserByUsername(username);

        // 1. 주문 확정
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        order.confirmOrder();

        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);

        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 취소
     *
     * <p>여러 도메인 서비스를 조정하여 주문 취소 유스케이스를 완성합니다.</p>
     * <p>주문을 취소하고 차감된 재고를 원복하며 포인트를 환불합니다.</p>
     *
     * @param orderId  주문 ID
     * @param username 사용자명 (포인트 환불용)
     * @return 취소된 주문 정보
     */
    @Transactional
    public OrderInfo cancelOrder(Long orderId, String username) {
        // 1. 사용자 조회
        UserEntity user = userService.getUserByUsername(username);

        // 2. 주문 조회
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());

        // 3. 도메인 서비스: 주문 취소 처리 (도메인 로직)
        List<OrderItemEntity> orderItems = orderService.cancelOrderDomain(order);

        // 4. 재고 원복
        for (OrderItemEntity orderItem : orderItems) {
            productService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
        }

        // 5. 쿠폰 원복
        orderItems.stream()
                .filter(orderItem -> orderItem.getCouponId() != null)
                .forEach(orderItem -> {
                    CouponEntity coupon = couponService.getCouponByIdAndUserId(
                            orderItem.getCouponId(),
                            order.getUserId()
                    );
                    if (!coupon.isUsed()) {
                        throw new IllegalStateException("취소하려는 주문의 쿠폰이 사용된 상태가 아닙니다.");
                    }
                    couponService.revertCoupon(coupon);
                });

        // 6. 포인트 환불 (할인 후 금액으로)
        pointService.refund(username, order.getFinalTotalAmount());

        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 ID로 주문 조회
     *
     * @param username 사용자명
     * @param orderId  주문 ID
     * @return 주문 정보
     */
    public OrderInfo getOrderById(String username, Long orderId) {
        UserEntity user = userService.getUserByUsername(username);
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 사용자 ID로 주문 요약 목록을 페이징하여 조회합니다.
     * 주문 항목 정보는 포함하지 않고 항목 개수만 포함합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 요약 정보 목록
     */
    public Page<OrderSummary> getOrderSummariesByUserId(Long userId, Pageable pageable) {
        Page<OrderEntity> orderPage = orderService.getOrdersByUserId(userId, pageable);
        return orderPage.map(order -> {
            int itemCount = orderService.countOrderItems(order.getId());
            return OrderSummary.from(order, itemCount);
        });
    }

    /**
     * 사용자 ID와 주문 상태로 주문 요약 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param status   주문 상태
     * @param pageable 페이징 정보
     * @return 페이징된 주문 요약 정보 목록
     */
    public Page<OrderSummary> getOrderSummariesByUserIdAndStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable) {
        Page<OrderEntity> orderPage = orderService.getOrdersByUserIdAndStatus(userId, status, pageable);
        return orderPage.map(order -> {
            int itemCount = orderService.countOrderItems(order.getId());
            return OrderSummary.from(order, itemCount);
        });
    }

    /**
     * 주문 ID로 주문 요약 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 요약 정보
     */
    public OrderSummary getOrderSummaryById(Long orderId, String username) {
        UserEntity user = userService.getUserByUsername(username);

        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        int itemCount = orderService.countOrderItems(orderId);
        return OrderSummary.from(order, itemCount);
    }

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param username 사용자명
     * @param pageable 페이징 정보
     * @return 주문 항목 정보 페이지
     */
    public Page<OrderItemInfo> getOrderItemsByOrderId(
            Long orderId,
            String username,
            Pageable pageable) {
        UserEntity user = userService.getUserByUsername(username);

        // 주문 존재 여부 확인
        orderService.getOrderByIdAndUserId(orderId, user.getId());

        // 주문 항목 페이징 조회
        Page<OrderItemEntity> orderItemsPage =
                orderService.getOrderItemsByOrderId(orderId, pageable);
        return orderItemsPage.map(OrderItemInfo::from);
    }
}
