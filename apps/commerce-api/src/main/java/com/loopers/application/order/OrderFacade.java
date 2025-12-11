package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.order.event.OrderEventPublisher;
import com.loopers.domain.payment.CommercePayment;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.shared.util.IdempotencyService;
import com.loopers.shared.util.IdempotencyType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 주문 생성 (Choreography 패턴 - 비동기 이벤트 기반)
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

        // 5. 주문 생성 이벤트 발행 (Choreography 패턴)
        // StockEventListener가 이 이벤트를 구독하여 재고 차감 시작
        OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
            userId, 
            savedOrder.getId(), 
            request
        );
        orderEventPublisher.publishOrderCreated(orderCreatedEvent);
        
        log.info("OrderCreatedEvent 발행 완료 - orderId: {}", savedOrder.getId());
    }
    
    /**
     * PG 콜백 처리
     * 낙관적 락을 통해 동시성 문제를 해결합니다.
     */
    @Transactional
    public OrderInfo callbackOrder(OrderDto.PgCallbackRequest request) {

        Long orderId = Long.parseLong(request.orderId());

        // 실패하면 원복 처리 및 400 에러 보내기
        if(request.status() == PaymentDto.PaymentStatus.FAILED) {
            // [Choreography] 주문 보상 이벤트 발행
            paymentEventPublisher.publishPaymentProcessingFailed(new PaymentEvents.ProcessingFailed(orderId, request.reason()));
            Order savedOrder = orderService.saveFailedOrder(orderId, request.reason());
            return OrderInfo.from(savedOrder);
        }

        // 결제 정보 조회 및 완료 처리
        CommercePayment commercePayment = paymentService.findByTransactionKey(request.transactionKey());
        paymentService.saveSuccessPayment(commercePayment.getTransactionKey());

        // 주문 완료 처리하기
        Order savedOrder = orderService.saveSuccessOrder(orderId);

        return OrderInfo.from(savedOrder);
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

