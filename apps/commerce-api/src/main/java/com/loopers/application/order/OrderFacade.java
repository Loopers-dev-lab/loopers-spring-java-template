package com.loopers.application.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
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
import com.loopers.domain.tracking.UserBehaviorTracker;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 주문 Facade
 * <p>
 * 주문 생성, 확정, 취소 등의 유스케이스를 조정합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;
    private final CouponService couponService;
    private final PaymentFacade paymentFacade;
    private final UserBehaviorTracker behaviorTracker;


    /**
     * 주문 생성
     * <p>
     * 여러 도메인 서비스를 조정하여 주문 생성 유스케이스를 완성합니다.
     *
     * @param command 주문 생성 명령
     * @return 생성된 주문 정보
     * @throws IllegalArgumentException 재고 부족 또는 주문 불가능한 경우
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo createOrderByPoint(@jakarta.validation.Valid OrderFacadeDtos.OrderCreateCommand command) {
        // 1. 주문자 정보 조회 (락 적용)
        UserEntity user = userService.findByUsernameWithLock(command.username());

        // 2. 주문 항목을 상품 ID 기준으로 정렬 (교착 상태 방지)
        List<OrderFacadeDtos.OrderItemCommand> sortedItems = command.orderItems().stream()
                .sorted(Comparator.comparing(OrderFacadeDtos.OrderItemCommand::productId))
                .toList();

        // 3. 상품 검증 및 준비 (재고 확인, 락 적용)
        List<ProductEntity> orderableProducts = new ArrayList<>();
        List<CouponEntity> coupons = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (OrderFacadeDtos.OrderItemCommand itemCommand : sortedItems) {
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


        IntStream.range(0, orderableProducts.size())
                .forEach(i -> productService.deductStock(orderableProducts.get(i), quantities.get(i)));

        // 쿠폰 사용 처리 (도메인 로직)
        couponService.consumeCoupons(coupons, creationResult.order().getId());

        // 7. 유저 행동 추적 (주문 생성)
        behaviorTracker.trackOrderCreate(
                user.getId(),
                creationResult.order().getId(),
                "POINT", // 포인트 결제
                creationResult.order().getFinalTotalAmount().doubleValue(),
                creationResult.orderItems().size()
        );

        // 8. 주문 정보 반환
        return OrderFacadeDtos.OrderInfo.from(creationResult.order(), creationResult.orderItems());
    }

    /**
     * 포인트 결제용 분리된 커맨드 오버로드
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo createOrderByPoint(
            @jakarta.validation.Valid OrderFacadeDtos.PointOrderCreateCommand command) {
        OrderFacadeDtos.OrderCreateCommand legacy = OrderFacadeDtos.OrderCreateCommand.builder()
                .username(command.username())
                .orderItems(command.orderItems())
                .paymentType(com.loopers.domain.payment.PaymentType.POINT)
                .build();
        return createOrderByPoint(legacy);
    }

    /**
     * 카드 결제용 주문 생성
     * <p>
     * 포인트 차감 없이 주문만 생성합니다.
     * 재고 차감, 쿠폰 사용은 주문 생성 시 처리하고, 결제는 별도 API로 진행합니다.
     *
     * @param command 주문 생성 명령
     * @return 생성된 주문 정보 (PENDING 상태)
     * @throws IllegalArgumentException 재고 부족 또는 주문 불가능한 경우
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo createOrderForCardPayment(
            @jakarta.validation.Valid OrderFacadeDtos.OrderCreateCommand command) {
        // 1. 주문자 정보 조회 (락 적용)
        UserEntity user = userService.findByUsernameWithLock(command.username());

        // 2. 주문 항목을 상품 ID 기준으로 정렬 (교착 상태 방지)
        List<OrderFacadeDtos.OrderItemCommand> sortedItems = command.orderItems().stream()
                .sorted(Comparator.comparing(OrderFacadeDtos.OrderItemCommand::productId))
                .toList();

        // 3. 상품 검증 및 준비 (재고 확인, 락 적용)
        List<ProductEntity> orderableProducts = new ArrayList<>();
        List<CouponEntity> coupons = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (OrderFacadeDtos.OrderItemCommand itemCommand : sortedItems) {
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

        // 7. 재고 차감
        IntStream.range(0, orderableProducts.size())
                .forEach(i -> productService.deductStock(orderableProducts.get(i), quantities.get(i)));

        // 쿠폰 사용 처리 (도메인 로직)
        couponService.consumeCoupons(coupons, creationResult.order().getId());

        // 유저 행동 추적 (주문 생성)
        behaviorTracker.trackOrderCreate(
                user.getId(),
                creationResult.order().getId(),
                "CARD", // 카드 결제
                creationResult.order().getFinalTotalAmount().doubleValue(),
                creationResult.orderItems().size()
        );


        // 8. 주문 정보 반환 (PENDING 상태)
        return OrderFacadeDtos.OrderInfo.from(creationResult.order(), creationResult.orderItems());
    }


    /**
     * 카드 결제와 함께 주문 생성 (통합 처리)
     * <p>
     * 주문 생성 + PG 결제 요청을 한 번에 처리합니다.
     *
     * @param command 주문 생성 명령 (카드 정보 포함)
     * @return 주문 정보 + 결제 정보
     */
    @Transactional
    public OrderFacadeDtos.OrderWithPaymentInfo createOrderWithCardPayment(
            @jakarta.validation.Valid OrderFacadeDtos.OrderCreateCommand command) {
        // 1. 주문 생성 (재고 차감, 쿠폰 사용, 포인트 차감 안 함)
        OrderFacadeDtos.OrderInfo orderInfo = createOrderForCardPayment(command);

        // 2. 결제 요청 (주문 ID 사용)
        PaymentCommand paymentCommand =
                PaymentCommand.builder()
                        .username(command.username())
                        .orderId(orderInfo.id())
                        .orderNumber(orderInfo.orderNumber())
                        .cardType(command.cardInfo().cardType())
                        .cardNo(command.cardInfo().cardNo())
                        .amount(orderInfo.finalTotalAmount())
                        .callbackUrl(command.cardInfo().callbackUrl())
                        .build();

        PaymentInfo paymentInfo = paymentFacade.processPayment(paymentCommand);

        // 3. 주문 + 결제 정보 반환
        return new OrderFacadeDtos.OrderWithPaymentInfo(orderInfo, paymentInfo);
    }

    /**
     * 카드 결제 통합 처리 분리된 커맨드 오버로드
     */
    @Transactional
    public OrderFacadeDtos.OrderWithPaymentInfo createOrderWithCardPayment(
            @jakarta.validation.Valid OrderFacadeDtos.CardOrderCreateCommand command) {
        OrderFacadeDtos.OrderCreateCommand.CardPaymentInfo legacyCard = new OrderFacadeDtos.OrderCreateCommand.CardPaymentInfo(
                command.cardInfo().cardType(),
                command.cardInfo().cardNo(),
                command.cardInfo().callbackUrl()
        );
        OrderFacadeDtos.OrderCreateCommand legacy = OrderFacadeDtos.OrderCreateCommand.builder()
                .username(command.username())
                .orderItems(command.orderItems())
                .paymentType(com.loopers.domain.payment.PaymentType.CARD)
                .cardInfo(legacyCard)
                .build();
        return createOrderWithCardPayment(legacy);
    }

    /**
     * 주문 확정
     * <p>
     * 주문을 확정합니다. (재고는 이미 주문 생성 시 차감되었음)
     *
     * @param orderId  주문 ID
     * @param username 사용자명
     * @return 확정된 주문 정보
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo confirmOrderByPoint(Long orderId, String username) {
        UserEntity user = userService.getUserByUsername(username);

        // 1. 주문 확정
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        order.confirmOrder();

        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);

        return OrderFacadeDtos.OrderInfo.from(order, orderItems);
    }

    /**
     * 결제 성공 시 주문 확정 처리
     * <p>
     * PG 결제 성공 콜백을 받았을 때 호출됩니다.
     * 포인트 차감 없이 주문만 확정합니다. (이미 카드 결제로 처리됨)
     *
     * @param orderId 주문 ID
     * @return 확정된 주문 정보
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo confirmOrderByPayment(Long orderId, Long userId) {
        // 1. 주문 확정 (도메인 이벤트 + 데이터 플랫폼 이벤트 발행)
        OrderEntity order = orderService.getOrderByOrderNumberAndUserId(orderId, userId);
        order.confirmWithEvent();

        // 2. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);

        return OrderFacadeDtos.OrderInfo.from(order, orderItems);
    }

    /**
     * 결제 실패 시 주문 취소 및 보상 처리
     * <p>
     * PG 결제 실패 콜백을 받았을 때 호출됩니다.
     * 재고 복원, 쿠폰 복원 등의 보상 트랜잭션을 수행합니다.
     *
     * @param orderId 주문 ID
     * @return 취소된 주문 정보
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo cancelOrderByPaymentFailure(Long orderId, Long userId) {
        // 1. 주문 조회 및 취소 (도메인 이벤트 + 데이터 플랫폼 이벤트 발행)
        OrderEntity order = orderService.getOrderByOrderNumberAndUserId(orderId, userId);
        List<OrderItemEntity> orderItems = orderService.cancelOrderDomainWithEvent(order, "결제 실패");

        // 2. 재고 원복
        for (OrderItemEntity orderItem : orderItems) {
            productService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
        }

        // 3. 쿠폰 원복
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

        // 4. 포인트는 차감하지 않았으므로 환불하지 않음
        return OrderFacadeDtos.OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 취소
     * <p>
     * 여러 도메인 서비스를 조정하여 주문 취소 유스케이스를 완성합니다.
     * 주문을 취소하고 차감된 재고를 원복하며 포인트를 환불합니다.
     *
     * @param orderId  주문 ID
     * @param username 사용자명 (포인트 환불용)
     * @return 취소된 주문 정보
     */
    @Transactional
    public OrderFacadeDtos.OrderInfo cancelOrderByPoint(Long orderId, String username) {
        // 1. 사용자 조회
        UserEntity user = userService.getUserByUsername(username);

        // 2. 주문 조회
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());

        // 3. 도메인 서비스: 주문 취소 처리 (도메인 로직, 도메인 이벤트 + 데이터 플랫폼 이벤트 발행)
        List<OrderItemEntity> orderItems = orderService.cancelOrderDomainWithEvent(order, "사용자 요청");

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

        return OrderFacadeDtos.OrderInfo.from(order, orderItems);
    }

    /**
     * 주문 ID로 주문 조회
     *
     * @param username 사용자명
     * @param orderId  주문 ID
     * @return 주문 정보
     */
    public OrderFacadeDtos.OrderInfo getOrderById(String username, Long orderId) {
        UserEntity user = userService.getUserByUsername(username);
        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);
        return OrderFacadeDtos.OrderInfo.from(order, orderItems);
    }

    /**
     * 사용자 ID로 주문 요약 목록을 페이징하여 조회합니다.
     * 주문 항목 정보는 포함하지 않고 항목 개수만 포함합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 요약 정보 목록
     */
    public Page<OrderFacadeDtos.OrderSummary> getOrderSummariesByUserId(Long userId, Pageable pageable) {
        Page<OrderEntity> orderPage = orderService.getOrdersByUserId(userId, pageable);
        return orderPage.map(order -> {
            int itemCount = orderService.countOrderItems(order.getId());
            return OrderFacadeDtos.OrderSummary.from(order, itemCount);
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
    public Page<OrderFacadeDtos.OrderSummary> getOrderSummariesByUserIdAndStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable) {
        Page<OrderEntity> orderPage = orderService.getOrdersByUserIdAndStatus(userId, status, pageable);
        return orderPage.map(order -> {
            int itemCount = orderService.countOrderItems(order.getId());
            return OrderFacadeDtos.OrderSummary.from(order, itemCount);
        });
    }

    /**
     * 주문 ID로 주문 요약 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 요약 정보
     */
    public OrderFacadeDtos.OrderSummary getOrderSummaryById(Long orderId, String username) {
        UserEntity user = userService.getUserByUsername(username);

        OrderEntity order = orderService.getOrderByIdAndUserId(orderId, user.getId());
        int itemCount = orderService.countOrderItems(orderId);
        return OrderFacadeDtos.OrderSummary.from(order, itemCount);
    }

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param username 사용자명
     * @param pageable 페이징 정보
     * @return 주문 항목 정보 페이지
     */
    public Page<OrderFacadeDtos.OrderItemInfo> getOrderItemsByOrderId(
            Long orderId,
            String username,
            Pageable pageable) {
        UserEntity user = userService.getUserByUsername(username);

        // 주문 존재 여부 확인
        orderService.getOrderByIdAndUserId(orderId, user.getId());

        // 주문 항목 페이징 조회
        Page<OrderItemEntity> orderItemsPage =
                orderService.getOrderItemsByOrderId(orderId, pageable);
        return orderItemsPage.map(OrderFacadeDtos.OrderItemInfo::from);
    }
}
