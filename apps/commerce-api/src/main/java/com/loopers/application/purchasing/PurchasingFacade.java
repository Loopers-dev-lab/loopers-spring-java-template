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
     * 주문을 생성한다.
     * <p>
     * 1. 사용자 조회 및 존재 여부 검증<br>
     * 2. 상품 재고 검증 및 차감<br>
     * 3. 사용자 포인트 검증 및 차감<br>
     * 4. 주문 저장
     * </p>
     * <p>
     * <b>동시성 제어 전략:</b>
     * <ul>
     *   <li><b>PESSIMISTIC_WRITE 사용 근거:</b> Lost Update 방지 및 데이터 일관성 보장</li>
     *   <li><b>포인트 차감:</b> 동시 주문 시 포인트 중복 차감 방지 (금전적 손실 방지)</li>
     *   <li><b>재고 차감:</b> 동시 주문 시 재고 음수 방지 및 정확한 차감 보장 (재고 oversell 방지)</li>
     *   <li><b>Lock 범위 최소화:</b> PK/UNIQUE 인덱스 기반 조회로 Lock 범위 최소화</li>
     * </ul>
     * </p>
     * <p>
     * <b>DBA 설득 근거 (비관적 락 사용):</b>
     * <ul>
     *   <li><b>제한적 사용:</b> 전역이 아닌 금전적 손실 위험이 있는 특정 도메인에만 사용</li>
     *   <li><b>트랜잭션 최소화:</b> 트랜잭션 내부에 외부 I/O 없음, lock holding time 매우 짧음 (몇 ms)</li>
     *   <li><b>Lock 범위 최소화:</b> PK/UNIQUE 인덱스 기반 조회로 해당 행만 락 (Record Lock)</li>
     *   <li><b>애플리케이션 레벨 한계:</b> 애플리케이션 레벨로는 race condition을 완전히 방지할 수 없어서 DB 차원의 strong consistency 필요</li>
     *   <li><b>낙관적 락 기본 전략:</b> 쿠폰 사용은 낙관적 락 사용 (Hot Spot 대응)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Lock 생명주기:</b>
     * <ol>
     *   <li>SELECT ... FOR UPDATE 실행 시 락 획득</li>
     *   <li>트랜잭션 내에서 락 유지 (외부 I/O 없음, 매우 짧은 시간)</li>
     *   <li>트랜잭션 커밋/롤백 시 락 자동 해제</li>
     * </ol>
     * </p>
     *
     * @param userId 사용자 식별자 (로그인 ID)
     * @param commands 주문 상품 정보
     * @return 생성된 주문 정보
     */
    @Transactional
    public OrderInfo createOrder(String userId, List<OrderItemCommand> commands) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
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
     * 사용자 ID로 주문 목록을 조회한다.
     *
     * @param userId 사용자 식별자 (로그인 ID)
     * @return 주문 목록
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
     * 주문 ID로 단일 주문을 조회한다.
     *
     * @param userId 사용자 식별자 (로그인 ID)
     * @param orderId 주문 ID
     * @return 주문 정보
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

    private User loadUser(String userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    /**
     * 비관적 락을 사용하여 사용자를 조회합니다.
     * <p>
     * 포인트 차감 등 동시성 제어가 필요한 경우 사용합니다.
     * </p>
     * <p>
     * <b>전제 조건:</b> userId는 상위 계층에서 이미 null/blank 검증이 완료되어야 합니다.
     * </p>
     *
     * @param userId 사용자 ID (null이 아니고 비어있지 않아야 함)
     * @return 조회된 사용자
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
    private User loadUserForUpdate(String userId) {
        User user = userRepository.findByUserIdForUpdate(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    /**
     * 주문 명령에서 쿠폰 코드를 추출합니다.
     *
     * @param commands 주문 명령 목록
     * @return 쿠폰 코드 (없으면 null)
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
     * 주문 아이템 목록으로부터 소계 금액을 계산합니다.
     *
     * @param orderItems 주문 아이템 목록
     * @return 계산된 소계 금액
     */
    private Integer calculateSubtotal(List<OrderItem> orderItems) {
        return orderItems.stream()
            .mapToInt(item -> item.getPrice() * item.getQuantity())
            .sum();
    }
}

