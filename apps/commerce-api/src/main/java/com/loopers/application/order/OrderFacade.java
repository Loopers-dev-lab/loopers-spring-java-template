package com.loopers.application.order;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.interfaces.api.payment.PaymentMethod;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);

    private final OrderService orderService;
    private final ProductService productService;
    private final StockService stockService;

    private final PaymentFacade paymentFacade;

    private final OrderExternalSystemSender orderExternalSystemSender;

    @Transactional
    public Order createOrder(Long userId, OrderV1Dto.OrderCreateRequest request) {

        // 주문 정보 세팅
        Order order = prepareOrder(userId, request.items());

        // 주문 저장
        orderService.createOrder(order);

        // 결제 준비중 상태 변경
        order.changeToPending();

        // 결제
        // 카드 결제 처리 (비동기)
        paymentFacade.pay(order, request.payment(), request.cardPaymentInfo());

        // 포인트 결제 성공처리 (동기)
        if (request.payment() == PaymentMethod.POINT) {
            onPaymentSuccess(order.getId());
        }

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

    /**
     * 결제 성공 시 재고 차감 및 주문 정보 외부 시스템 전송
     * 주문 상태 변경 (PENDING -> PAID)
     * @param orderId
     */
    @Transactional
    public void onPaymentSuccess(Long orderId) {
        Order order = orderService.findOrderById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다."));

        // 재고 차감
        stockService.decreaseStocks(order.getItems());

        // 주문 PAID 처리
        order.changeToPaid();

        // 주문 정보 외부 시스템 전송 (실패해도 주문 트랜잭션은 롤백하지 않도록 catch)
        try {
            orderExternalSystemSender.send(order);
        } catch (Exception e) {
            log.error("외부 시스템으로의 주문 전송 실패, 주문 ID: {}", order.getId(), e);
        }
    }

    /**
     * 결제 실패 시 주문 상태 변경 (PENDING -> FAILED)
     * @param orderId
     */
    @Transactional
    public void onPaymentFailure(Long orderId) {
        Order order = orderService.findOrderById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다."));

        // 주문 PENDING 처리
        order.changeToFailed();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long userId, Long id) {
        return orderService.findOrder(userId, id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long userId) {
        return orderService.findAll(userId);
    }
}
