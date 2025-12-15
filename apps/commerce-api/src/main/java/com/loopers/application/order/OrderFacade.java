package com.loopers.application.order;

import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private final OrderService orderService;
    private final ProductService productService;
    private final StockService stockService;
    private final PaymentService paymentService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Long userId, OrderV1Dto.OrderCreateRequest request) {

        // 주문 정보 세팅
        Order order = prepareOrder(userId, request.items());

        // 1. 주문 생성 (CREATED)
        orderService.createOrder(order);

        // 2. 재고 차감
        stockService.decreaseStocks(order.getItems());

        // 3. 주문 상태 변경 : 결제 대기 (CREATED -> PENDING)
        orderService.markPending(order.getId());

        // 4. 결제 정보 저장
        paymentService.savePayment(Payment.create(order, request.payment()));

        // 5. 이벤트 발행
        eventPublisher.publishEvent(OrderCreatedEvent.of(order.getId(), userId));

        return order;
    }

    public Order prepareOrder(Long userId, List<OrderItemInfo> items) {

        Map<Long, Product> productsById = productService.getProductsMapByIds(
                items.stream()
                        .map(OrderItemInfo::productId)
                        .toList()
        );

        // 주문 생성
        Order order = Order.create(userId);

        // 주문 아이템 세팅
        for (OrderItemInfo info : items) {
            Product product = productsById.get(info.productId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다. productId: " + info.productId());
            }

            order.addItem(
                    info.productId(),
                    info.quantity(),
                    Money.of(product.getPrice().getAmount()),
                    Money.of(product.getPrice().getAmount()).multiply(info.quantity())
            );
        }

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long userId, Long id) {
        return orderService.findOrderWithItems(userId, id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long userId) {
        return orderService.findAll(userId);
    }
}
