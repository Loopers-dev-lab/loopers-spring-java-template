package com.loopers.application.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.*;
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
     * Create a new order, validate items, apply coupons, deduct points and stock, and return the created order.
     *
     * Validations include product availability per requested quantity and coupon usage; coupons are consumed
     * and points are deducted before the order and its items are persisted. Order items are processed in
     * a stable order to avoid deadlocks.
     *
     * @param command contains the username and the list of order items (each may include an optional couponId)
     * @return the created OrderInfo composed of the persisted order and its items
     * @throws IllegalArgumentException if a product cannot be ordered due to insufficient stock or if a coupon is already used
     */
    @Transactional
    public OrderInfo createOrder(OrderCreateCommand command) {
        // 1. 주문자 정보 조회
        UserEntity user = userService.findByUsernameWithLock(command.username());

        // 2. 주문 상품 검증 및 총 주문 금액 계산
        List<ProductEntity> orderableProducts = new ArrayList<>();
        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // 주문 항목을 상품 ID 기준으로 정렬하여 교착 상태 방지
        List<OrderItemCommand> sortedItems = command.orderItems().stream()
                .sorted(Comparator.comparing(OrderItemCommand::productId))
                .toList();

        for (OrderItemCommand itemCommand : sortedItems) {
            // 상품 정보 조회 (재고 잠금 적용)
            ProductEntity product = productService.getProductDetailLock(itemCommand.productId());

            // 상품 주문 가능 여부 확인 (재고 부족 시 예외 발생)
            if (!product.canOrder(itemCommand.quantity())) {
                throw new IllegalArgumentException(
                        String.format("주문할 수 없는 상품입니다. 상품 ID: %d, 요청 수량: %d, 현재 재고: %d",
                                product.getId(), itemCommand.quantity(), product.getStockQuantity())
                );
            }

            // 주문 가능한 상품 목록에 추가
            orderableProducts.add(product);
            BigDecimal itemTotal;
            if (itemCommand.couponId() != null) {
                CouponEntity coupon = couponService.getCouponByIdAndUserId(itemCommand.couponId(), user.getId());
                if (coupon.isUsed()) {
                    throw new IllegalArgumentException("이미 사용된 쿠폰입니다.");
                }
                BigDecimal basePrice = product.getSellingPrice().multiply(BigDecimal.valueOf(itemCommand.quantity()));
                BigDecimal discount = coupon.calculateDiscount(basePrice);
                itemTotal = basePrice.subtract(discount);
                couponService.consumeCoupon(coupon);
            } else {
                itemTotal = product.getSellingPrice().multiply(BigDecimal.valueOf(itemCommand.quantity()));
            }
            totalOrderAmount = totalOrderAmount.add(itemTotal);

        }

        // 3. 주문 금액만큼 포인트 차감
        pointService.use(user, totalOrderAmount);

        // 4. 주문 엔티티 생성 (사용자 ID와 총 금액으로)
        OrderEntity order = orderService.createOrder(
                new OrderDomainCreateRequest(user.getId(), totalOrderAmount)
        );

        // 5. 주문 항목 생성 및 재고 차감 처리
        List<OrderItemEntity> orderItems = new ArrayList<>();
        IntStream.range(0, sortedItems.size()).forEach(i -> {
            OrderItemCommand itemCommand = sortedItems.get(i);
            ProductEntity product = orderableProducts.get(i);

            // 재고 차감 (도메인 서비스를 통해 처리)
            productService.deductStock(product, itemCommand.quantity());

            // 주문 항목 엔티티 생성 (주문 ID, 상품 ID, 수량, 가격으로)
            OrderItemEntity orderItem = orderService.createOrderItem(
                    new OrderItemDomainCreateRequest(
                            order.getId(),
                            product.getId(),
                            itemCommand.couponId() != null ? itemCommand.couponId() : null,
                            itemCommand.quantity(),
                            product.getSellingPrice()
                    )
            );
            orderItems.add(orderItem);
        });

        // 주문 정보 반환 (주문 엔티티와 항목 목록으로 구성)
        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 확정
     *
     * <p>주문을 확정합니다. (재고는 이미 주문 생성 시 차감되었음)</p>
     *
     * @param orderId 주문 ID
     * @return 확정된 주문 정보
     */
    @Transactional
    public OrderInfo confirmOrder(Long orderId) {
        // 1. 주문 확정
        OrderEntity order = orderService.getOrderById(orderId);
        order.confirmOrder();

        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId);

        return OrderInfo.from(order, orderItems);
    }

    /**
     * Cancel an order, restore deducted stock, revert any used coupons, and refund points to the user.
     *
     * @param orderId the ID of the order to cancel
     * @param username the username to credit the refunded points to
     * @return the cancelled order information including its items
     */
    @Transactional
    public OrderInfo cancelOrder(Long orderId, String username) {
        // 1. 주문 취소
        OrderEntity order = orderService.getOrderById(orderId);
        order.cancelOrder();

        // 2. 주문 항목 조회
        /**
         * 데드락 시나리오:
         * 스레드 A: [상품 1, 상품 2] 순서로 락 획득 → 락 1 획득 → 락 2 대기 중
         * 스레드 B: [상품 2, 상품 1] 순서로 락 획득 → 락 2 획득 → 락 1 대기 중
         * 결과: DB 수준의 원형 대기(circular wait) 발생
         * 이를 방지하기 위해, 주문 항목을 productId 기준으로 정렬한 뒤 처리하세요:
         */
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(orderId)
                .stream().sorted(Comparator.comparing(OrderItemEntity::getProductId))
                .toList();

        // 3. 재고 원복
        for (OrderItemEntity orderItem : orderItems) {
            productService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
            if (orderItem.getCouponId() != null) {
                CouponEntity coupon = couponService.getCouponByIdAndUserId(orderItem.getCouponId(), order.getUserId());
                if (!coupon.isUsed()) {
                    throw new IllegalStateException("취소하려는 주문의 쿠폰이 사용된 상태가 아닙니다.");
                }
                couponService.revertCoupon(coupon);
            }
        }

        // 4. 포인트 환불
        pointService.charge(username, order.getTotalAmount());

        return OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 ID로 주문 조회
     *
     * @param orderId 주문 ID
     * @return 주문 정보
     */
    public OrderInfo getOrderById(Long orderId) {
        OrderEntity order = orderService.getOrderById(orderId);
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
    public OrderSummary getOrderSummaryById(Long orderId) {
        OrderEntity order = orderService.getOrderById(orderId);
        int itemCount = orderService.countOrderItems(orderId);
        return OrderSummary.from(order, itemCount);
    }

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param pageable 페이징 정보
     * @return 주문 항목 정보 페이지
     */
    public Page<OrderItemInfo> getOrderItemsByOrderId(
            Long orderId,
            Pageable pageable) {
        // 주문 존재 여부 확인
        orderService.getOrderById(orderId);

        // 주문 항목 페이징 조회
        Page<OrderItemEntity> orderItemsPage =
                orderService.getOrderItemsByOrderId(orderId, pageable);
        return orderItemsPage.map(OrderItemInfo::from);
    }
}