package com.loopers.domain.order;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.event.EventType;
import com.loopers.domain.order.vo.PgTransactionResponse;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(Map<Product, Integer> productQuantityMap, Long userId, DiscountResult discountResult, PaymentMethod paymentMethod) {
        List<OrderItem> orderItems = new ArrayList<>();
        productQuantityMap.forEach((product, quantity) -> orderItems.add(OrderItem.create(
                product.getId(),
                product.getName(),
                quantity,
                product.getPrice()
        )));
        Order order = Order.create(userId, orderItems, discountResult, paymentMethod);
        order = orderRepository.save(order);

        eventPublisher.publishOrderCreated(
                OrderEvent.createOrderCreatedEvent(
                        order.getId(),
                        order.getOrderId(),
                        order.getUserId(),
                        order.getCouponId(),
                        order.getFinalPrice(),
                        order.getPaymentMethod()
                )
        );
        return order;
    }

    public Order getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    @Transactional
    public void setPgPaymentInfo(Long orderId, String cardType, String cardNo, String callbackUrl) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
        order.setPaymentInfo(cardType, cardNo, callbackUrl);
        orderRepository.save(order);
    }

    @Transactional
    public void markPaidByPoint(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
        order.markPaidByPoint();
        eventPublisher.publishOrderPaid(OrderEvent.createOrderPaidEvent(EventType.CREATED, order));
        orderRepository.save(order);
    }

    @Transactional
    public void updateStatusByPgResponse(Long orderId, PgTransactionResponse pgResponse) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
        order.updateOrderStatus(pgResponse);
        orderRepository.save(order);
        
        // 결제 완료 이벤트 발행 (PAID 상태인 경우)
        if (order.getStatus() == OrderStatus.PAID) {
            eventPublisher.publishOrderPaid(OrderEvent.createOrderPaidEvent(EventType.UPDATED, order));
        }
    }

    public Order getOrderByIdAndUserIdForUpdate(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public Page<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return orderRepository.findByUserIdAndDeletedAtIsNull(userId, PageRequest.of(page, size, sort));
    }

    @Transactional(readOnly = true)
    public List<Order> findPendingPaymentOrdersBefore(ZonedDateTime before) {
        return orderRepository.findPendingPaymentOrdersBefore(before);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    /**
     * 재결제 시도 횟수 증가
     */
    @Transactional
    public void incrementRetryCount(Long orderId) {
        Order order = getOrderById(orderId);
        order.incrementRetryCount();
        orderRepository.save(order);
    }

    /**
     * 타임아웃으로 인한 실패 처리
     */
    @Transactional
    public void markAsFailedByTimeout(Long orderId, String reason) {
        Order order = getOrderById(orderId);
        order.markAsFailedByTimeout(reason);
        orderRepository.save(order);
    }

    /**
     * PG 응답으로 주문 상태 업데이트 (기존 메서드와 동일하지만 명확성을 위해 유지)
     */
    @Transactional
    public void updatePaymentStatus(Long orderId, OrderStatus newStatus, String transactionKey, String reason) {
        Order order = getOrderById(orderId);
        order.updatePaymentStatus(newStatus, transactionKey, reason);
        orderRepository.save(order);
    }
}
