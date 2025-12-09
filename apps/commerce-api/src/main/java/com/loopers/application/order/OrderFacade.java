package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.order.event.OrderEventPublisher;
import com.loopers.domain.payment.CommercePayment;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentProcessingFailedEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.util.IdempotencyService;
import com.loopers.support.util.IdempotencyType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);

    private final OrderService orderService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final OrderEventPublisher orderEventPublisher;
    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * 주문 생성 (Event-driven)
     */
    @Transactional
    public void createOrder(Long userId, OrderDto.CreateOrderRequest request) {

        // 1. 주문 요청 유효성 검사
        request.validate();

        // 2. 멱등성 키 체크
        String idempotencyKey = request.generateIdempotentKey(userId);
        if (idempotencyService.checkAndSet(IdempotencyType.ORDER, idempotencyKey)) {
            throw new CoreException(
                ErrorType.CONFLICT,
                "이미 주문이 처리되었습니다."
            );
        }

        // 3. Product 정보 조회
        Map<Long, Product> productMap = new HashMap<>();
        request.items().forEach(itemRequest -> {
            Product product = productService.findById(itemRequest.productId());
            productMap.put(itemRequest.productId(), product);
        });

        // 4. 주문 생성 (초기 상태: PENDING)
        Order order = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .userId(userId)
                .build();
        
        // 4-1. OrderItem 리스트 생성 및 적용 (주문 시점의 상품 정보를 스냅샷으로 저장)
        request.items().forEach(itemRequest -> {
            Product product = productMap.get(itemRequest.productId());
            order.addOrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    itemRequest.quantity()
            );
        });

        // 4-2. 주문 저장
        Order savedOrder = orderService.saveOrder(order);

        // 5. 주문 생성 이벤트 발행
        orderEventPublisher.publishOrderCreated(new OrderCreatedEvent(userId, savedOrder.getId(), request));
    }
    
    /**
     * PG 콜백 처리
     * 낙관적 락을 통해 동시성 문제를 해결합니다.
     */
    @Transactional
    public OrderInfo callbackOrder(OrderDto.PgCallbackRequest request) {
        try {
            // 주문 정보 조회
            Order order = orderService.findOrderById(Long.parseLong(request.orderId()));

            // 멱등성 체크 (이미 처리된 주문이면 바로 반환)
            Optional<OrderInfo> idempotencyResult = checkIdempotency(order);
            if (idempotencyResult.isPresent()) {
                return idempotencyResult.get();
            }

            // PENDING 상태가 아니면 예외 발생
            if (order.getOrderStatus() != OrderStatus.PENDING) {
                throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PENDING 상태의 주문만 콜백을 처리할 수 있습니다. 현재 상태 - Order: " + order.getOrderStatus()
                );
            }

            // 실패하면 원복 처리 및 400 에러 보내기
            if(request.status() == PaymentDto.PaymentStatus.FAILED) {
                // [SAGA] 주문 보상 이벤트 발행
                paymentEventPublisher.publishPaymentProcessingFailed(new PaymentProcessingFailedEvent(order.getId(), request.reason()));
                throw new CoreException(
                        ErrorType.BAD_REQUEST,
                        request.reason()
                );
            }

            // 결제 정보 조회 및 완료 처리
            CommercePayment commercePayment = paymentService.findByTransactionKey(request.transactionKey());
            paymentService.saveSuccessPayment(commercePayment.getTransactionKey());

            // 주문 완료 처리하기
            Order savedOrder = orderService.saveSuccessOrder(order.getId());

            return OrderInfo.from(savedOrder);

        } catch (OptimisticLockingFailureException e) {
            log.warn("낙관적 락 실패 - orderId: {}, transactionKey: {}, 다른 트랜잭션에서 이미 처리되었을 수 있습니다.",
                request.orderId(), request.transactionKey());

            // 최신 상태로 다시 조회하여 확인
            Order freshOrder = orderService.findOrderById(Long.parseLong(request.orderId()));
            return checkIdempotency(freshOrder).orElseThrow(() ->
                new CoreException(
                    ErrorType.CONFLICT,
                    "동시 요청으로 인해 처리에 실패했습니다. 잠시 후 다시 시도해주세요."
                )
            );
        }
    }

    /**
     * 멱등성 체크: 이미 처리된 주문인지 확인 (완료 또는 실패)
     * @param order 주문
     * @return 이미 완료된 주문이면 OrderInfo를 담은 Optional, 그렇지 않으면 빈 Optional
     * @throws CoreException 이미 실패한 주문인 경우
     */
    private Optional<OrderInfo> checkIdempotency(Order order) {
        if (order.getOrderStatus() == OrderStatus.PAYMENT_FAILED) {
            log.warn("이미 결제 실패한 주문입니다 - orderId: {}", order.getId());
            throw new CoreException(
                ErrorType.BAD_REQUEST,
                "이미 결제 실패한 주문입니다."
            );
        }

        if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
            log.info("주문이 이미 완료되었습니다 - orderId: {}, 멱등성 보장", order.getId());
            return Optional.of(OrderInfo.from(order));
        }

        return Optional.empty();
    }

    /**
     * 단일 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderInfo getOrder(Long orderId) {
        Order order = orderService.findOrderById(orderId);
        return OrderInfo.from(order);
    }

    /**
     * 유저의 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(Long userId) {
        List<Order> orders = orderService.findOrdersByUserId(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }
}

