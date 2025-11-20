package com.loopers.application.purchasing;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.discount.CouponDiscountStrategyFactory;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 구매 파사드.
 * <p>
 * 주문 생성과 결제(포인트 차감), 재고 조정, 외부 연동을 조율한다.
 * </p>
 */
@RequiredArgsConstructor
@Component
public class PurchasingFacade {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponDiscountStrategyFactory couponDiscountStrategyFactory;

    /**
     * Create a new order for the given user from the provided order item commands.
     *
     * @param userId  the user's identifier (login ID)
     * @param commands  list of order item commands; must contain at least one distinct product entry
     * @return  an OrderInfo representing the persisted order including applied coupon and discount
     * @throws CoreException if the request is invalid (ErrorType.BAD_REQUEST for empty or duplicate items or invalid coupon usage),
     *                       if required entities are missing (ErrorType.NOT_FOUND for user, product, or coupon), or
     *                       if a coupon cannot be applied due to concurrent usage (ErrorType.CONFLICT)
     */
    @Transactional
    public OrderInfo createOrder(String userId, List<OrderItemCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 아이템은 1개 이상이어야 합니다.");
        }

        // 비관적 락을 사용하여 사용자 조회 (포인트 차감 시 동시성 제어)
        // - userId는 UNIQUE 인덱스가 있어 Lock 범위 최소화 (Record Lock만 적용)
        // - Lost Update 방지: 동시 주문 시 포인트 중복 차감 방지 (금전적 손실 방지)
        // - 트랜잭션 내부에 외부 I/O 없음, lock holding time 매우 짧음
        User user = loadUserForUpdate(userId);

        Set<Long> productIds = new HashSet<>();
        List<Product> products = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemCommand command : commands) {
            if (!productIds.add(command.productId())) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                    String.format("상품이 중복되었습니다. (상품 ID: %d)", command.productId()));
            }

            // 비관적 락을 사용하여 상품 조회 (재고 차감 시 동시성 제어)
            // - id는 PK 인덱스가 있어 Lock 범위 최소화 (Record Lock만 적용)
            // - Lost Update 방지: 동시 주문 시 재고 음수 방지 및 정확한 차감 보장 (재고 oversell 방지)
            // - 트랜잭션 내부에 외부 I/O 없음, lock holding time 매우 짧음
            Product product = productRepository.findByIdForUpdate(command.productId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                    String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", command.productId())));
            products.add(product);

            orderItems.add(OrderItem.of(
                product.getId(),
                product.getName(),
                product.getPrice(),
                command.quantity()
            ));
        }

        // 쿠폰 처리 (있는 경우)
        String couponCode = extractCouponCode(commands);
        Integer discountAmount = 0;
        if (couponCode != null && !couponCode.isBlank()) {
            discountAmount = applyCoupon(user.getId(), couponCode, calculateSubtotal(orderItems));
        }

        Order order = Order.of(user.getId(), orderItems, couponCode, discountAmount);

        decreaseStocksForOrderItems(order.getItems(), products);
        deductUserPoint(user, order.getTotalAmount());
        order.complete();

        products.forEach(productRepository::save);
        userRepository.save(user);

        Order savedOrder = orderRepository.save(order);

        return OrderInfo.from(savedOrder);
    }

    /**
     * 주문을 취소하고 포인트를 환불하며 재고를 원복한다.
     *
     * @param order 주문 엔티티
     * @param user 사용자 엔티티
     */
    @Transactional
    public void cancelOrder(Order order, User user) {
        if (order == null || user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "취소할 주문과 사용자 정보는 필수입니다.");
        }

        List<Product> products = order.getItems().stream()
            .map(item -> productRepository.findById(item.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                    String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", item.getProductId()))))
            .toList();

        order.cancel();
        increaseStocksForOrderItems(order.getItems(), products);
        user.receivePoint(Point.of((long) order.getTotalAmount()));

        products.forEach(productRepository::save);
        userRepository.save(user);
        orderRepository.save(order);
    }

    /**
     * Retrieves all orders belonging to the user identified by userId.
     *
     * @param userId the user's login identifier
     * @return a list of OrderInfo for the user's orders; empty list if the user has no orders
     */
    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String userId) {
        User user = loadUser(userId);
        List<Order> orders = orderRepository.findAllByUserId(user.getId());
        return orders.stream()
            .map(OrderInfo::from)
            .toList();
    }

    /**
     * Retrieves the specified order for the requesting user and verifies ownership.
     *
     * @param userId  ID of the requesting user
     * @param orderId ID of the order to retrieve
     * @return        an OrderInfo representing the requested order
     * @throws CoreException if the order does not exist or does not belong to the user (NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public OrderInfo getOrder(String userId, Long orderId) {
        User user = loadUser(userId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(user.getId())) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }

        return OrderInfo.from(order);
    }

    private void decreaseStocksForOrderItems(List<OrderItem> items, List<Product> products) {
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, product -> product));

        for (OrderItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND,
                    String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", item.getProductId()));
            }
            product.decreaseStock(item.getQuantity());
        }
    }

    private void increaseStocksForOrderItems(List<OrderItem> items, List<Product> products) {
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, product -> product));

        for (OrderItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND,
                    String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", item.getProductId()));
            }
            product.increaseStock(item.getQuantity());
        }
    }

    private void deductUserPoint(User user, Integer totalAmount) {
        if (Objects.requireNonNullElse(totalAmount, 0) <= 0) {
            return;
        }
        user.deductPoint(Point.of(totalAmount.longValue()));
    }

    /**
     * Retrieve the User with the given userId or fail if no such user exists.
     *
     * @param userId the user's unique identifier
     * @return the matching User
     * @throws CoreException with ErrorType.NOT_FOUND if no user is found for the given id
     */
    private User loadUser(String userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    /**
     * Loads the user with a pessimistic lock suitable for update operations.
     *
     * Use this when concurrent control is required (for example, when deducting points).
     *
     * @param userId the identifier of the user to load
     * @return the loaded User
     * @throws CoreException if the user does not exist (error type NOT_FOUND)
     */
    private User loadUserForUpdate(String userId) {
        User user = userRepository.findByUserIdForUpdate(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    /**
     * Extracts the first non-blank coupon code found in the given order item commands.
     *
     * @param commands order item commands to scan for a coupon code
     * @return the first non-blank coupon code, or {@code null} if none is present
     */
    private String extractCouponCode(List<OrderItemCommand> commands) {
        return commands.stream()
            .filter(cmd -> cmd.couponCode() != null && !cmd.couponCode().isBlank())
            .map(OrderItemCommand::couponCode)
            .findFirst()
            .orElse(null);
    }

    /**
     * 쿠폰을 적용하여 할인 금액을 계산하고 쿠폰을 사용 처리합니다.
     * <p>
     * <b>동시성 제어 전략:</b>
     * <ul>
     *   <li><b>OPTIMISTIC_LOCK 사용 근거:</b> 쿠폰 중복 사용 방지, Hot Spot 대응</li>
     *   <li><b>@Version 필드:</b> UserCoupon 엔티티의 version 필드를 통해 자동으로 낙관적 락 적용</li>
     *   <li><b>동시 사용 시:</b> 한 명만 성공하고 나머지는 OptimisticLockException 발생</li>
     *   <li><b>사용 목적:</b> 동일 쿠폰으로 여러 기기에서 동시 주문해도 한 번만 사용되도록 보장</li>
     * </ul>
     * </p>
     *
     * @param userId 사용자 ID
     * @param couponCode 쿠폰 코드
     * @param subtotal 주문 소계 금액
     * @return 할인 금액
     * @throws CoreException 쿠폰을 찾을 수 없거나 사용 불가능한 경우, 동시 사용으로 인한 충돌 시
     */
    private Integer applyCoupon(Long userId, String couponCode, Integer subtotal) {
        // 쿠폰 존재 여부 확인
        Coupon coupon = couponRepository.findByCode(couponCode)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                String.format("쿠폰을 찾을 수 없습니다. (쿠폰 코드: %s)", couponCode)));

        // 낙관적 락을 사용하여 사용자 쿠폰 조회 (동시성 제어)
        // @Version 필드가 있어 자동으로 낙관적 락이 적용됨
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponCodeForUpdate(userId, couponCode)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                String.format("사용자가 소유한 쿠폰을 찾을 수 없습니다. (쿠폰 코드: %s)", couponCode)));

        // 쿠폰 사용 가능 여부 확인
        if (!userCoupon.isAvailable()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("이미 사용된 쿠폰입니다. (쿠폰 코드: %s)", couponCode));
        }

        // 쿠폰 사용 처리
        userCoupon.use();

        // 할인 금액 계산 (전략 패턴 사용)
        Integer discountAmount = coupon.calculateDiscountAmount(subtotal, couponDiscountStrategyFactory);

        try {
            // 사용자 쿠폰 저장 (version 체크 자동 수행)
            // 다른 트랜잭션이 먼저 수정했다면 OptimisticLockException 발생
            userCouponRepository.save(userCoupon);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 충돌: 다른 트랜잭션이 먼저 쿠폰을 사용함
            throw new CoreException(ErrorType.CONFLICT,
                String.format("쿠폰이 이미 사용되었습니다. (쿠폰 코드: %s)", couponCode));
        }

        return discountAmount;
    }

    /**
     * Computes the subtotal amount for the given list of order items.
     *
     * @param orderItems the list of order items to sum
     * @return the subtotal amount (sum of each item's price multiplied by its quantity)
     */
    private Integer calculateSubtotal(List<OrderItem> orderItems) {
        return orderItems.stream()
            .mapToInt(item -> item.getPrice() * item.getQuantity())
            .sum();
    }
}
