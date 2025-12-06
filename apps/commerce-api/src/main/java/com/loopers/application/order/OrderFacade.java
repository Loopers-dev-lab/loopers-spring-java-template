package com.loopers.application.order;

import com.loopers.application.payment.PaymentCompensationService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.order.strategy.PaymentContext;
import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.domain.order.strategy.PaymentStrategyFactory;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderDomainService orderDomainService;
    private final ProductDomainService productDomainService;
    private final ProductCacheService productCacheService;
    private final PaymentDomainService paymentDomainService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentCompensationService paymentCompensationService;

    public record PaymentInfo(
        PaymentType paymentType,
        String cardType,
        String cardNo
    ) {
    }

    @Transactional
    public OrderInfo createOrder(String userId, List<OrderDto.OrderItemRequest> itemRequests, String cardType, String cardNo) {
        PaymentType paymentType = determinePaymentType(cardType);
        PaymentInfo paymentInfo = new PaymentInfo(paymentType, cardType, cardNo);
        return createOrder(userId, itemRequests, paymentInfo);
    }

    @Transactional
    public OrderInfo createOrder(
        String userId,
        List<OrderDto.OrderItemRequest> itemRequests,
        PaymentInfo paymentInfo
    ) {
        validateItem(itemRequests);

        // 락 순서
        List<OrderDto.OrderItemRequest> sortedItems = itemRequests.stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId))
                .toList();

        // 상품 재고 차감
        long totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderDto.OrderItemRequest itemRequest : sortedItems) {

            Product product = productDomainService.decreaseStock(
                    itemRequest.productId(),
                    itemRequest.quantity()
            );

            // 재고 변경되었으니 해당 상품의 detail 캐시 무효화
            productCacheService.deleteProductDetail(itemRequest.productId());

            totalAmount += product.getPrice() * itemRequest.quantity();

            orderItems.add(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            ));
        }

        // 주문 생성
        Order order = orderDomainService.createOrder(userId, orderItems, totalAmount);

        // Payment 생성
        Payment payment = paymentDomainService.createPayment(
            order.getId(),
            userId,
            totalAmount,
            paymentInfo.paymentType()
        );

        // 결제 실행
        order.startPayment();

        // 포인트 결제는 동기 처리 (트랜잭션 내에서 즉시 완료)
        if (paymentInfo.paymentType() == PaymentType.POINT_ONLY) {
            PaymentContext context = PaymentContext.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .userId(userId)
                .totalAmount(totalAmount)
                .pointAmount(totalAmount)
                .cardAmount(0)
                .cardType(paymentInfo.cardType())
                .cardNo(paymentInfo.cardNo())
                .build();

            PaymentStrategy strategy = paymentStrategyFactory.create(paymentInfo.paymentType());
            strategy.executePayment(context);

            order.confirm();
            paymentDomainService.markAsSuccess(payment.getId(), "internal-payment");
        } else {
            // 카드 결제는 트랜잭션 커밋 후 비동기 실행
            Long orderId = order.getId();
            Long paymentId = payment.getId();
            long finalTotalAmount = totalAmount;

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    PaymentContext context = PaymentContext.builder()
                        .orderId(orderId)
                        .paymentId(paymentId)
                        .userId(userId)
                        .totalAmount(finalTotalAmount)
                        .pointAmount(0)
                        .cardAmount(finalTotalAmount)
                        .cardType(paymentInfo.cardType())
                        .cardNo(paymentInfo.cardNo())
                        .build();

                    try {
                        PaymentStrategy strategy = paymentStrategyFactory.create(paymentInfo.paymentType());
                        strategy.executePayment(context);
                    } catch (Exception e) {
                        log.error("PG payment request failed after commit. orderId: {}, paymentId: {}, error: {}",
                                orderId, paymentId, e.getMessage(), e);
                        paymentCompensationService.compensateFailedPayment(
                                userId, orderId, paymentId, e.getMessage());
                    }
                }
            });
        }

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String userId) {
        List<Order> orders = orderDomainService.getOrders(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrder(String userId, Long orderId) {
        Order order = orderDomainService.getOrder(userId, orderId);
        return OrderInfo.from(order);
    }

    private PaymentType determinePaymentType(String cardType) {
        if (cardType != null && !cardType.isBlank()) {
            return PaymentType.CARD_ONLY;
        }
        return PaymentType.POINT_ONLY;
    }

    @Transactional
    public void handlePaymentFailure(String userId, Long orderId) {
        Order order = orderDomainService.getOrder(userId, orderId);

        if (order.getStatus() != OrderStatus.PAYING) {
            log.warn("Order is not in PAYING status. Skipping failure handling. orderId: {}, status: {}",
                    orderId, order.getStatus());
            return;
        }

        List<OrderItem> items = new ArrayList<>(order.getOrderItems());
        items.sort(Comparator.comparing(OrderItem::getProductId).reversed());

        for (OrderItem item : items) {
            productDomainService.increaseStock(item.getProductId(), item.getQuantity());
            productCacheService.deleteProductDetail(item.getProductId());
        }

        order.fail();
    }

    private static void validateItem(List<OrderDto.OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "하나 이상의 상품을 주문해야 합니다."
            );
        }
    }
}
