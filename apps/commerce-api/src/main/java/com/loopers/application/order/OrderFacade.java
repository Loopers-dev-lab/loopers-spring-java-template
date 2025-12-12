package com.loopers.application.order;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 애플리케이션 파사드
 * - 트랜잭션 경계 관리
 * - 핵심 주문 로직 처리 (재고 차감, 포인트 차감)
 * - 부가 로직은 이벤트로 분리 (쿠폰 사용)
 */
@Service
public class OrderFacade {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CouponService couponService;
    private final PointService pointService;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderFacade(
            OrderService orderService,
            OrderRepository orderRepository,
            CouponService couponService,
            PointService pointService,
            ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.couponService = couponService;
        this.pointService = pointService;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 주문 생성 (트랜잭션 및 동시성 제어 적용)
     * - 핵심 로직: 재고 차감, 포인트 차감, 주문 생성
     * - 부가 로직(쿠폰 사용)은 이벤트로 분리하여 비동기 처리
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderCommand command) {
        String userId = command.getUserId();
        List<OrderService.OrderItemRequest> orderItemRequests = command.getOrderItems();
        int usedPoints = command.getUsedPoints();
        Long couponId = command.getCouponId();

        // 1. 상품 조회 및 재고 확인 (비관적 락 적용)
        List<OrderItem> validatedOrderItems = orderItemRequests.stream()
                .map(this::validateAndCreateOrderItem)
                .collect(Collectors.toList());

        // 2. 총 금액 계산
        BigDecimal totalAmount = validatedOrderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 쿠폰 할인 금액 계산 (쿠폰 사용은 이벤트로 분리)
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponId != null) {
            // 쿠폰 조회 및 할인 금액 계산만 수행 (사용 처리는 이벤트에서)
            Coupon coupon = couponService.useCoupon(couponId, userId);
            couponDiscount = coupon.calculateDiscount(totalAmount);
        }

        // 4. 포인트 차감 (동시성 제어 - JPA 더티체킹)
        BigDecimal amountAfterCoupon = totalAmount.subtract(couponDiscount);
        if (usedPoints > 0) {
            if (BigDecimal.valueOf(usedPoints).compareTo(amountAfterCoupon) > 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "사용 포인트가 쿠폰 할인 후 금액을 초과할 수 없습니다");
            }
            pointService.consume(userId, (long) usedPoints);
        }

        // 5. 재고 차감 (비관적 락 적용)
        for (OrderService.OrderItemRequest request : orderItemRequests) {
            Product product = productRepository.findByIdWithLock(request.getProductId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));

            product.decreaseStock(request.getQuantity());
            productRepository.save(product);
        }

        // 6. 주문 생성 및 저장
        Order order = Order.create(userId, validatedOrderItems, couponDiscount, usedPoints, couponId);
        Order savedOrder = orderRepository.save(order);

        // 7. 주문 생성 이벤트 발행 (쿠폰 사용, 데이터 플랫폼 전송 등)
        eventPublisher.publishEvent(OrderCreatedEvent.from(savedOrder));

        return OrderResponse.from(savedOrder);
    }

    /**
     * 주문 항목 검증 및 생성
     */
    private OrderItem validateAndCreateOrderItem(OrderService.OrderItemRequest request) {
        // 상품 조회
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));

        // 재고 확인
        if (!product.canPurchase(request.getQuantity())) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    String.format("재고가 부족합니다. 상품: %s, 요청: %d, 재고: %d",
                            product.getName(),
                            request.getQuantity(),
                            product.getStock())
            );
        }

        // OrderItem 생성
        return OrderItem.create(
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.getQuantity()
        );
    }

    /**
     * 주문 취소 (트랜잭션)
     */
    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        orderService.cancelOrder(orderId, userId);
    }
}
