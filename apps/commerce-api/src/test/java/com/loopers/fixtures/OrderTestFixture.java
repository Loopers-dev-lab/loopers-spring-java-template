package com.loopers.fixtures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderStatus;

/**
 * 주문 관련 테스트 픽스처 클래스
 * 통합 테스트에서 편리하게 사용할 수 있는 주문 객체 생성 메서드들을 제공합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 20.
 */
public class OrderTestFixture {

    // ==================== 기본값 ====================
    public static final String DEFAULT_USERNAME = "testuser";
    public static final BigDecimal DEFAULT_TOTAL_AMOUNT = new BigDecimal("10000.00");
    public static final Integer DEFAULT_QUANTITY = 1;
    public static final Integer DEFAULT_LARGE_QUANTITY = 10;

    // ==================== 기본 OrderCreateCommand 생성 ====================

    /**
     * 기본값으로 OrderCreateCommand 생성
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createDefaultOrderCommand(String username, Long productId) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, DEFAULT_QUANTITY, null)
        ));
    }

    /**
     * 기본값으로 OrderCreateCommand 생성 (쿠폰 포함)
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param couponId  쿠폰 ID
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createOrderCommandWithCoupon(String username, Long productId, Long couponId) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, DEFAULT_QUANTITY, couponId)
        ));
    }

    /**
     * 커스텀 OrderCreateCommand 생성
     *
     * @param username   사용자명
     * @param orderItems 주문 항목 리스트
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createOrderCommand(String username, List<OrderItemCommand> orderItems) {
        return OrderCreateCommand.builder()
                .username(username)
                .orderItems(orderItems)
                .build();
    }

    // ==================== 단일 상품 주문 ====================

    /**
     * 단일 상품 주문 생성
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createSingleProductOrder(String username, Long productId, Integer quantity) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, quantity, null)
        ));
    }

    /**
     * 단일 상품 주문 생성 (쿠폰 포함)
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @param couponId  쿠폰 ID
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createSingleProductOrderWithCoupon(
            String username,
            Long productId,
            Integer quantity,
            Long couponId
    ) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, quantity, couponId)
        ));
    }

    // ==================== 다중 상품 주문 ====================

    /**
     * 다중 상품 주문 생성
     *
     * @param username   사용자명
     * @param productIds 상품 ID 리스트
     * @param quantities 수량 리스트
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createMultiProductOrder(
            String username,
            List<Long> productIds,
            List<Integer> quantities
    ) {
        List<OrderItemCommand> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            items.add(createOrderItem(productIds.get(i), quantities.get(i), null));
        }
        return createOrderCommand(username, items);
    }

    /**
     * 다중 상품 주문 생성 (쿠폰 포함)
     *
     * @param username   사용자명
     * @param productIds 상품 ID 리스트
     * @param quantities 수량 리스트
     * @param couponIds  쿠폰 ID 리스트 (null 가능)
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createMultiProductOrderWithCoupons(
            String username,
            List<Long> productIds,
            List<Integer> quantities,
            List<Long> couponIds
    ) {
        List<OrderItemCommand> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long couponId = (couponIds != null && i < couponIds.size()) ? couponIds.get(i) : null;
            items.add(createOrderItem(productIds.get(i), quantities.get(i), couponId));
        }
        return createOrderCommand(username, items);
    }

    /**
     * 다중 상품 주문 생성 (Map 기반)
     *
     * @param username           사용자명
     * @param productQuantityMap 상품ID와 수량 맵
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createMultiProductOrder(
            String username,
            Map<Long, Integer> productQuantityMap
    ) {
        List<OrderItemCommand> items = new ArrayList<>();
        productQuantityMap.forEach((productId, quantity) ->
                items.add(createOrderItem(productId, quantity, null))
        );
        return createOrderCommand(username, items);
    }

    /**
     * 다중 상품 + 다중 쿠폰 주문 (Map 기반)
     *
     * @param username         사용자명
     * @param productCouponMap 상품ID와 쿠폰ID 맵 (쿠폰 없으면 null)
     * @param quantityMap      상품ID와 수량 맵
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createMultiProductOrderWithCouponMap(
            String username,
            Map<Long, Long> productCouponMap,
            Map<Long, Integer> quantityMap
    ) {
        List<OrderItemCommand> items = new ArrayList<>();
        productCouponMap.forEach((productId, couponId) -> {
            Integer quantity = quantityMap.getOrDefault(productId, DEFAULT_QUANTITY);
            items.add(createOrderItem(productId, quantity, couponId));
        });
        return createOrderCommand(username, items);
    }

    // ==================== OrderItemCommand 생성 ====================

    /**
     * OrderItemCommand 생성
     *
     * @param productId 상품 ID
     * @param quantity  수량
     * @param couponId  쿠폰 ID (null 가능)
     * @return OrderItemCommand
     */
    public static OrderItemCommand createOrderItem(Long productId, Integer quantity, Long couponId) {
        return OrderItemCommand.builder()
                .productId(productId)
                .quantity(quantity)
                .couponId(couponId)
                .build();
    }

    /**
     * OrderItemCommand 생성 (쿠폰 없음)
     *
     * @param productId 상품 ID
     * @param quantity  수량
     * @return OrderItemCommand
     */
    public static OrderItemCommand createOrderItem(Long productId, Integer quantity) {
        return createOrderItem(productId, quantity, null);
    }

    // ==================== 통합 테스트 시나리오 헬퍼 ====================

    /**
     * 통합 테스트용 - 간단한 주문 생성
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createSimpleOrder(String username, Long productId, Integer quantity) {
        return createSingleProductOrder(username, productId, quantity);
    }

    /**
     * 통합 테스트용 - 쿠폰 적용 단일 상품 주문
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @param couponId  쿠폰 ID
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createOrderWithSingleCoupon(
            String username,
            Long productId,
            Integer quantity,
            Long couponId
    ) {
        return createSingleProductOrderWithCoupon(username, productId, quantity, couponId);
    }

    /**
     * 통합 테스트용 - 동시성 테스트를 위한 동일 주문 생성
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @param couponId  쿠폰 ID (null 가능)
     * @param count     생성할 주문 개수
     * @return OrderCreateCommand 리스트
     */
    public static List<OrderCreateCommand> createConcurrentOrders(
            String username,
            Long productId,
            Integer quantity,
            Long couponId,
            int count
    ) {
        List<OrderCreateCommand> commands = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            commands.add(couponId != null
                    ? createOrderWithSingleCoupon(username, productId, quantity, couponId)
                    : createSimpleOrder(username, productId, quantity)
            );
        }
        return commands;
    }

    /**
     * 통합 테스트용 - 서로 다른 쿠폰을 사용하는 동시 주문 생성
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량
     * @param couponIds 쿠폰 ID 리스트
     * @return OrderCreateCommand 리스트
     */
    public static List<OrderCreateCommand> createConcurrentOrdersWithDifferentCoupons(
            String username,
            Long productId,
            Integer quantity,
            List<Long> couponIds
    ) {
        List<OrderCreateCommand> commands = new ArrayList<>();
        for (Long couponId : couponIds) {
            commands.add(createOrderWithSingleCoupon(username, productId, quantity, couponId));
        }
        return commands;
    }

    /**
     * 통합 테스트용 - 재고 부족 시나리오를 위한 대량 주문
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  수량 (재고보다 많게)
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createOrderExceedingStock(
            String username,
            Long productId,
            Integer quantity
    ) {
        return createSimpleOrder(username, productId, quantity);
    }

    /**
     * 통합 테스트용 - 포인트 부족 시나리오를 위한 고액 주문
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @param quantity  대량 수량
     * @return OrderCreateCommand
     */
    public static OrderCreateCommand createHighValueOrder(
            String username,
            Long productId,
            Integer quantity
    ) {
        return createSimpleOrder(username, productId, quantity);
    }

    // ==================== 검증 헬퍼 메서드 ====================

    /**
     * 주문 상태 검증
     *
     * @param order          주문 엔티티
     * @param expectedStatus 예상 상태
     */
    public static void assertOrderStatus(OrderEntity order, OrderStatus expectedStatus) {
        Assertions.assertThat(order.getStatus()).isEqualTo(expectedStatus);
    }

    /**
     * 주문 금액 검증
     *
     * @param order          주문 엔티티
     * @param expectedAmount 예상 금액
     */
    public static void assertOrderAmount(OrderEntity order, BigDecimal expectedAmount) {
        Assertions.assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedAmount);
    }

    /**
     * 주문 항목 개수 검증
     *
     * @param orderItems    주문 항목 리스트
     * @param expectedCount 예상 개수
     */
    public static void assertOrderItemCount(List<OrderItemEntity> orderItems, int expectedCount) {
        Assertions.assertThat(orderItems).hasSize(expectedCount);
    }

    /**
     * 주문 항목 상세 검증
     *
     * @param orderItem         주문 항목
     * @param expectedProductId 예상 상품 ID
     * @param expectedQuantity  예상 수량
     * @param expectedUnitPrice 예상 단가
     */
    public static void assertOrderItem(
            OrderItemEntity orderItem,
            Long expectedProductId,
            Integer expectedQuantity,
            BigDecimal expectedUnitPrice
    ) {
        Assertions.assertThat(orderItem.getProductId()).isEqualTo(expectedProductId);
        Assertions.assertThat(orderItem.getQuantity()).isEqualTo(expectedQuantity);
        Assertions.assertThat(orderItem.getUnitPrice()).isEqualByComparingTo(expectedUnitPrice);
    }

    /**
     * 주문 총액 계산
     *
     * @param orderItems 주문 항목 리스트
     * @return 계산된 총액
     */
    public static BigDecimal calculateTotalAmount(List<OrderItemEntity> orderItems) {
        return orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 주문 항목 총액 검증
     *
     * @param orderItem          주문 항목
     * @param expectedTotalPrice 예상 총액
     */
    public static void assertOrderItemTotalPrice(OrderItemEntity orderItem, BigDecimal expectedTotalPrice) {
        BigDecimal actualTotalPrice = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        Assertions.assertThat(actualTotalPrice).isEqualByComparingTo(expectedTotalPrice);
    }

    /**
     * 주문 항목 리스트에서 특정 상품의 항목 찾기
     *
     * @param orderItems 주문 항목 리스트
     * @param productId  상품 ID
     * @return 찾은 주문 항목 (없으면 null)
     */
    public static OrderItemEntity findOrderItemByProductId(List<OrderItemEntity> orderItems, Long productId) {
        return orderItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 주문 항목 리스트에서 특정 상품들의 항목 찾기
     *
     * @param orderItems 주문 항목 리스트
     * @param productIds 상품 ID 리스트
     * @return 찾은 주문 항목 리스트
     */
    public static List<OrderItemEntity> findOrderItemsByProductIds(
            List<OrderItemEntity> orderItems,
            List<Long> productIds
    ) {
        return orderItems.stream()
                .filter(item -> productIds.contains(item.getProductId()))
                .toList();
    }

    /**
     * 주문 상태 변경 검증
     *
     * @param order      주문 엔티티
     * @param fromStatus 변경 전 상태
     * @param toStatus   변경 후 상태
     */
    public static void assertOrderStatusChange(OrderEntity order, OrderStatus fromStatus, OrderStatus toStatus) {
        Assertions.assertThat(order.getStatus()).isNotEqualTo(fromStatus);
        Assertions.assertThat(order.getStatus()).isEqualTo(toStatus);
    }

    /**
     * 주문 생성 시간 검증
     *
     * @param order 주문 엔티티
     */
    public static void assertOrderCreatedAtIsNotNull(OrderEntity order) {
        Assertions.assertThat(order.getCreatedAt()).isNotNull();
    }

    /**
     * 주문 ID 검증
     *
     * @param order 주문 엔티티
     */
    public static void assertOrderIdIsNotNull(OrderEntity order) {
        Assertions.assertThat(order.getId()).isNotNull();
    }

    /**
     * 주문 사용자 ID 검증
     *
     * @param order          주문 엔티티
     * @param expectedUserId 예상 사용자 ID
     */
    public static void assertOrderUserId(OrderEntity order, Long expectedUserId) {
        Assertions.assertThat(order.getUserId()).isEqualTo(expectedUserId);
    }

    /**
     * 주문 할인 금액 계산 (쿠폰 적용 전후 비교용)
     *
     * @param originalAmount   원래 금액
     * @param discountedAmount 할인된 금액
     * @return 할인 금액
     */
    public static BigDecimal calculateDiscountAmount(BigDecimal originalAmount, BigDecimal discountedAmount) {
        return originalAmount.subtract(discountedAmount);
    }

    /**
     * 할인율 계산 (백분율)
     *
     * @param originalAmount   원래 금액
     * @param discountedAmount 할인된 금액
     * @return 할인율 (0-100)
     */
    public static BigDecimal calculateDiscountRate(BigDecimal originalAmount, BigDecimal discountedAmount) {
        if (originalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = originalAmount.subtract(discountedAmount);
        return discount.multiply(new BigDecimal("100")).divide(originalAmount, 2, java.math.RoundingMode.HALF_UP);
    }
}
