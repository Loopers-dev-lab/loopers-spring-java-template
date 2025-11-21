package com.loopers.domain.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.order.dto.OrderCreationResult;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.domain.order.dto.OrderItemData;
import com.loopers.domain.order.dto.OrderItemDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 주문 도메인 서비스
 *
 * <p>주문과 주문 항목에 대한 핵심 비즈니스 로직을 처리합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    /**
     * 주문을 생성합니다.
     *
     * @param request 주문 생성 요청
     * @return 생성된 주문 엔티티
     */
    @Transactional
    public OrderEntity createOrder(OrderDomainCreateRequest request) {
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }

    /**
     * 주문과 주문 항목들을 생성합니다 (도메인 로직).
     *
     * <p>주문 생성의 핵심 도메인 로직을 처리합니다:</p>
     * <ul>
     *   <li>주문 항목별 금액 계산 (원가, 할인, 최종가)</li>
     *   <li>전체 주문 금액 집계</li>
     *   <li>주문 엔티티 및 주문 항목 엔티티 생성</li>
     * </ul>
     *
     * @param userId 사용자 ID
     * @param orderableProducts 주문 가능한 상품 목록 (재고 확인 완료)
     * @param coupons 적용할 쿠폰 목록 (주문 항목과 동일 순서)
     * @param quantities 주문 수량 목록 (주문 항목과 동일 순서)
     * @return 생성된 주문 엔티티와 주문 항목 목록
     */
    @Transactional
    public OrderCreationResult createOrderWithItems(
            Long userId,
            List<ProductEntity> orderableProducts,
            List<CouponEntity> coupons,
            List<Integer> quantities) {

        // 1. 항목별 금액 계산
        List<OrderItemData> itemDataList = new ArrayList<>();
        BigDecimal originalTotalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        for (int i = 0; i < orderableProducts.size(); i++) {
            ProductEntity product = orderableProducts.get(i);
            CouponEntity coupon = coupons.get(i);
            Integer quantity = quantities.get(i);

            // 항목별 원가 계산
            BigDecimal basePrice = product.getSellingPrice().multiply(BigDecimal.valueOf(quantity));

            // 항목별 할인 금액 계산
            BigDecimal itemDiscount = BigDecimal.ZERO;
            Long couponId = null;
            if (coupon != null) {
                itemDiscount = coupon.calculateDiscount(basePrice);
                couponId = coupon.getId();
            }

            itemDataList.add(new OrderItemData(
                    product.getId(),
                    couponId,
                    quantity,
                    product.getSellingPrice(),
                    itemDiscount
            ));

            originalTotalAmount = originalTotalAmount.add(basePrice);
            totalDiscountAmount = totalDiscountAmount.add(itemDiscount);
        }

        BigDecimal finalTotalAmount = originalTotalAmount.subtract(totalDiscountAmount);

        // 2. 주문 엔티티 생성
        OrderEntity order = createOrder(
                new OrderDomainCreateRequest(
                        userId,
                        originalTotalAmount,
                        totalDiscountAmount,
                        finalTotalAmount
                )
        );

        // 3. 주문 항목 엔티티 생성
        List<OrderItemEntity> orderItems = itemDataList.stream()
                .map(itemData -> createOrderItem(
                        new OrderItemDomainCreateRequest(
                                order.getId(),
                                itemData.productId(),
                                itemData.couponId(),
                                itemData.quantity(),
                                itemData.unitPrice(),
                                itemData.discountAmount()
                        )
                ))
                .toList();

        return new OrderCreationResult(order, orderItems);
    }

    /**
     * 주문 취소의 도메인 로직을 처리합니다.
     *
     * <p>주문 상태를 취소로 변경하고 주문 항목 목록을 반환합니다.</p>
     *
     * @param order 취소할 주문 엔티티
     * @return 정렬된 주문 항목 목록 (교착 상태 방지를 위해 productId 기준 정렬)
     */
    @Transactional
    public List<OrderItemEntity> cancelOrderDomain(OrderEntity order) {
        // 주문 취소 처리
        order.cancelOrder();

        // 주문 항목 조회 및 정렬 (교착 상태 방지)
        return getOrderItemsByOrderId(order.getId())
                .stream()
                .sorted(Comparator.comparing(OrderItemEntity::getProductId))
                .toList();
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @return 조회된 주문 엔티티
     * @throws CoreException 주문을 찾을 수 없는 경우
     */
    public OrderEntity getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("주문을 찾을 수 없습니다. (ID: %d)", orderId)
                ));
    }

    /**
     * 주문 항목을 생성합니다.
     *
     * @param request 주문 항목 생성 요청
     * @return 생성된 주문 항목 엔티티
     */
    @Transactional
    public OrderItemEntity createOrderItem(OrderItemDomainCreateRequest request) {
        OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);
        return orderItemRepository.save(orderItem);
    }

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    public List<OrderItemEntity> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * 주문을 삭제합니다 (소프트 삭제).
     *
     * @param orderId 주문 ID
     * @throws CoreException 주문을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteOrder(Long orderId, String username) {
        UserEntity user = userService.getUserByUsername(username);

        OrderEntity order = getOrderByIdAndUserId(orderId, user.getId());
        order.delete();

        List<OrderItemEntity> orderItemsByOrderId = getOrderItemsByOrderId(orderId);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemsByOrderId);
    }

    /**
     * 사용자 ID로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    public Page<OrderEntity> getOrdersByUserId(
            Long userId,
            Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 사용자 ID와 주문 상태로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param status   주문 상태
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    public Page<OrderEntity> getOrdersByUserIdAndStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable) {
        return orderRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * 주문 ID로 주문 항목 개수를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 개수
     */
    public int countOrderItems(Long orderId) {
        return orderItemRepository.countByOrderId(orderId);
    }

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 항목 목록
     */
    public Page<OrderItemEntity> getOrderItemsByOrderId(
            Long orderId,
            Pageable pageable) {
        return orderItemRepository.findByOrderId(orderId, pageable);
    }
}
