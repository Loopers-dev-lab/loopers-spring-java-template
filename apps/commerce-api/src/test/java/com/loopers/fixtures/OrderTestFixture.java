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
     * Create an OrderCreateCommand for a single product using the default quantity and no coupon.
     *
     * @param username  the username to associate with the order
     * @param productId the product ID to include as the single order item
     * @return an OrderCreateCommand containing one item with the default quantity and no coupon
     */
    public static OrderCreateCommand createDefaultOrderCommand(String username, Long productId) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, DEFAULT_QUANTITY, null)
        ));
    }

    /**
     * Create an OrderCreateCommand for a single product using the default quantity and the specified coupon.
     *
     * @return an OrderCreateCommand containing one order item for the given product with the default quantity and the provided coupon
     */
    public static OrderCreateCommand createOrderCommandWithCoupon(String username, Long productId, Long couponId) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, DEFAULT_QUANTITY, couponId)
        ));
    }

    /**
     * Create an OrderCreateCommand for the given user with the specified order items.
     *
     * @param username   the user name to associate with the order
     * @param orderItems the list of order item commands to include in the order
     * @return the constructed OrderCreateCommand
     */
    public static OrderCreateCommand createOrderCommand(String username, List<OrderItemCommand> orderItems) {
        return OrderCreateCommand.builder()
                .username(username)
                .orderItems(orderItems)
                .build();
    }

    // ==================== 단일 상품 주문 ====================

    /**
     * Create an OrderCreateCommand for a single product without a coupon.
     *
     * @param username  the username placing the order
     * @param productId the ID of the product to order
     * @param quantity  the quantity of the product
     * @return an OrderCreateCommand containing one order item for the specified product and quantity
     */
    public static OrderCreateCommand createSingleProductOrder(String username, Long productId, Integer quantity) {
        return createOrderCommand(username, List.of(
                createOrderItem(productId, quantity, null)
        ));
    }

    /**
     * Create an order command for a single product with an item-level coupon.
     *
     * @param username the username to associate with the order
     * @param productId the product identifier
     * @param quantity the quantity of the product
     * @param couponId the coupon identifier to apply to the item
     * @return an OrderCreateCommand representing the order with one item and the specified coupon
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
     * Builds an OrderCreateCommand for multiple products from parallel lists of product IDs and quantities.
     *
     * @param username   the username placing the order
     * @param productIds list of product IDs; each entry corresponds by index to the same-position quantity
     * @param quantities list of quantities corresponding by index to productIds
     * @return an OrderCreateCommand containing one OrderItemCommand per product/quantity pair
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
     * Create an order command containing multiple products with optional per-item coupons.
     *
     * @param username   the user name placing the order
     * @param productIds list of product IDs; each entry corresponds to an order item
     * @param quantities list of quantities; must have the same size as {@code productIds}
     * @param couponIds  optional list of coupon IDs; may be {@code null} or shorter than {@code productIds};
     *                   entries missing or {@code null} are treated as no coupon for that item
     * @return an {@code OrderCreateCommand} composed from the provided items and coupons
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
     * Create an OrderCreateCommand for multiple products using a map of product IDs to quantities.
     *
     * @param username           the username of the ordering user
     * @param productQuantityMap a map where each key is a product ID and each value is the desired quantity
     * @return an OrderCreateCommand containing one OrderItem per entry in the map
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
     * Create an order containing multiple products with optional per-item coupons using map inputs.
     *
     * @param username         the username placing the order
     * @param productCouponMap a map of productId -> couponId; a product's couponId may be `null` to indicate no coupon
     * @param quantityMap      a map of productId -> quantity; if a productId is missing, DEFAULT_QUANTITY is used
     * @return                 an OrderCreateCommand representing the order composed from the provided maps
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
     * Create an OrderItemCommand for the given product, quantity, and optional coupon.
     *
     * @param productId the product identifier
     * @param quantity the quantity to order
     * @param couponId the coupon identifier to apply, or null if none
     * @return an OrderItemCommand populated with the specified productId, quantity, and couponId
     */
    public static OrderItemCommand createOrderItem(Long productId, Integer quantity, Long couponId) {
        return OrderItemCommand.builder()
                .productId(productId)
                .quantity(quantity)
                .couponId(couponId)
                .build();
    }

    /**
     * Create an OrderItemCommand for the given product and quantity without a coupon.
     *
     * @param productId the product identifier
     * @param quantity  the quantity for the order item
     * @return an OrderItemCommand configured with the specified productId and quantity and no coupon
     */
    public static OrderItemCommand createOrderItem(Long productId, Integer quantity) {
        return createOrderItem(productId, quantity, null);
    }

    // ==================== 통합 테스트 시나리오 헬퍼 ====================

    /**
     * Create a simple single-product order command for integration tests.
     *
     * @return an OrderCreateCommand for the specified product and quantity
     */
    public static OrderCreateCommand createSimpleOrder(String username, Long productId, Integer quantity) {
        return createSingleProductOrder(username, productId, quantity);
    }

    /**
     * Create a single-product order command with a coupon for integration tests.
     *
     * @param username  the user name placing the order
     * @param productId the product identifier
     * @param quantity  the quantity of the product
     * @param couponId  the coupon identifier to apply to the order item
     * @return an OrderCreateCommand containing one item with the specified coupon
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
     * Create a list of identical order creation commands for concurrency testing.
     *
     * Each command represents a single-product order; if `couponId` is non-null the orders include that coupon.
     *
     * @param username  the username for each order
     * @param productId the product ID for each order
     * @param quantity  the quantity for the product in each order
     * @param couponId  optional coupon ID to apply to each order, or `null` for no coupon
     * @param count     number of orders to create
     * @return a list of `OrderCreateCommand` instances of length `count`
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
     * Create concurrent orders for integration tests using different coupons.
     *
     * @param username the username for all generated orders
     * @param productId the product id for all order items
     * @param quantity the quantity for the order item in each order
     * @param couponIds list of coupon ids; one order is created per coupon id
     * @return a list of OrderCreateCommand instances, one per coupon id, each applying the corresponding coupon
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
     * Create an order command representing a purchase that exceeds available stock for testing.
     *
     * @param username  the user placing the order
     * @param productId the ID of the product to order
     * @param quantity  the quantity to order; should be greater than the available stock
     * @return an OrderCreateCommand configured to create an order that exceeds stock
     */
    public static OrderCreateCommand createOrderExceedingStock(
            String username,
            Long productId,
            Integer quantity
    ) {
        return createSimpleOrder(username, productId, quantity);
    }

    /**
     * Builds an order intended to simulate a high-value purchase for point-insufficiency tests.
     *
     * @param username  the username placing the order
     * @param productId the product ID to include in the order
     * @param quantity  the large quantity to order (used to create a high-value order)
     * @return an OrderCreateCommand representing the high-value order
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
     * Verify that the given order's status matches the expected status.
     *
     * @param order         the order entity to check
     * @param expectedStatus the expected order status
     */
    public static void assertOrderStatus(OrderEntity order, OrderStatus expectedStatus) {
        Assertions.assertThat(order.getStatus()).isEqualTo(expectedStatus);
    }

    /**
     * Asserts that the order's total amount equals the given expected amount.
     *
     * @param order the order entity whose total amount will be validated
     * @param expectedAmount the expected total amount to compare against
     */
    public static void assertOrderAmount(OrderEntity order, BigDecimal expectedAmount) {
        Assertions.assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedAmount);
    }

    /**
     * Asserts that the provided list of order items contains exactly the expected number of entries.
     *
     * @param orderItems    the list of order item entities to check
     * @param expectedCount the expected number of items in the list
     */
    public static void assertOrderItemCount(List<OrderItemEntity> orderItems, int expectedCount) {
        Assertions.assertThat(orderItems).hasSize(expectedCount);
    }

    /**
     * Verify that an order item matches the expected product ID, quantity, and unit price.
     *
     * @param orderItem         the order item to validate
     * @param expectedProductId the expected product identifier
     * @param expectedQuantity  the expected quantity for the item
     * @param expectedUnitPrice the expected unit price for the item
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
     * Compute the total amount for the given order items.
     *
     * @param orderItems list of order item entities to include in the calculation
     * @return the sum of each item's `unitPrice * quantity`
     */
    public static BigDecimal calculateTotalAmount(List<OrderItemEntity> orderItems) {
        return orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Asserts that an order item's total price equals the expected total price.
     *
     * @param orderItem          the order item whose total (unit price × quantity) will be validated
     * @param expectedTotalPrice the expected total price to compare against
     */
    public static void assertOrderItemTotalPrice(OrderItemEntity orderItem, BigDecimal expectedTotalPrice) {
        BigDecimal actualTotalPrice = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        Assertions.assertThat(actualTotalPrice).isEqualByComparingTo(expectedTotalPrice);
    }

    /**
     * Finds the first order item that matches the given product ID.
     *
     * @param orderItems the list of order items to search
     * @param productId  the product ID to match
     * @return the matching OrderItemEntity, or `null` if no matching item is found
     */
    public static OrderItemEntity findOrderItemByProductId(List<OrderItemEntity> orderItems, Long productId) {
        return orderItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds order items whose product IDs are contained in the provided list.
     *
     * @param orderItems the list of order items to search
     * @param productIds the product IDs to match against each order item's productId
     * @return a list of order items with product IDs present in {@code productIds}, or an empty list if none match
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
     * Verifies that an order's status has changed from the specified previous status to the specified new status.
     *
     * @param order the order entity to inspect
     * @param fromStatus the expected previous status
     * @param toStatus the expected current status
     */
    public static void assertOrderStatusChange(OrderEntity order, OrderStatus fromStatus, OrderStatus toStatus) {
        Assertions.assertThat(order.getStatus()).isNotEqualTo(fromStatus);
        Assertions.assertThat(order.getStatus()).isEqualTo(toStatus);
    }

    /**
     * Asserts that the order's creation timestamp is not null.
     *
     * @param order the order entity to validate
     */
    public static void assertOrderCreatedAtIsNotNull(OrderEntity order) {
        Assertions.assertThat(order.getCreatedAt()).isNotNull();
    }

    /**
     * Asserts that the given order has a non-null ID.
     *
     * @param order the order entity whose ID is validated
     */
    public static void assertOrderIdIsNotNull(OrderEntity order) {
        Assertions.assertThat(order.getId()).isNotNull();
    }

    /**
     * Verifies that the given order's user ID matches the expected user ID.
     *
     * @param order the order entity whose user ID will be checked
     * @param expectedUserId the expected user ID value
     */
    public static void assertOrderUserId(OrderEntity order, Long expectedUserId) {
        Assertions.assertThat(order.getUserId()).isEqualTo(expectedUserId);
    }

    /**
     * Calculate the discount amount by comparing the original and discounted totals.
     *
     * @param originalAmount   the original total amount before discount
     * @param discountedAmount the total amount after discount
     * @return the discount amount (originalAmount minus discountedAmount)
     */
    public static BigDecimal calculateDiscountAmount(BigDecimal originalAmount, BigDecimal discountedAmount) {
        return originalAmount.subtract(discountedAmount);
    }

    /**
     * Calculate the discount rate as a percentage.
     *
     * @param originalAmount   the original total amount before discount
     * @param discountedAmount the amount after discount has been applied
     * @return the discount rate between 0 and 100, rounded to two decimal places (HALF_UP); returns 0 if {@code originalAmount} is zero
     */
    public static BigDecimal calculateDiscountRate(BigDecimal originalAmount, BigDecimal discountedAmount) {
        if (originalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = originalAmount.subtract(discountedAmount);
        return discount.multiply(new BigDecimal("100")).divide(originalAmount, 2, java.math.RoundingMode.HALF_UP);
    }
}