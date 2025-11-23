package com.loopers.application.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class); // Logger 필드 추가

    private final OrderService orderService;
    private final PointService pointService;
    private final OrderDomainService orderDomainService;
    private final ProductService productService;
    private final StockService stockService;

    private final OrderExternalSystemSender orderExternalSystemSender;

    @Transactional
    public Order createOrder(Long userId, List<OrderItemInfo> itemInfoList) {

        List<OrderItemContext> orderItemContexts = new ArrayList<>();

        // 주문 상품 조회 및 ID 정렬 : 데드락 방지
        List<Long> productIds = itemInfoList.stream()
                .map(OrderItemInfo::productId)
                .sorted()
                .collect(Collectors.toList());

        Map<Long, Product> productsById = productService.getProductsMapByIds(productIds);

        // 재고 정보 조회 + 비관락
        Map<Long, Stock> stocksByProductId = stockService.getStocksByProductIds(productIds);

        // 주문 생성
        Order order = Order.create(userId);

        // 주문 아이템 세팅
        for (OrderItemInfo info : itemInfoList) {
            Product product = productsById.get(info.productId());
            Stock stock = stocksByProductId.get(info.productId());

            Money unitPrice = Money.of(product.getPrice().getAmount());
            Money totalPrice = Money.of(product.getPrice().getAmount()).multiply(info.quantity());

            order.addItem(
                    info.productId(),
                    info.quantity(),
                    unitPrice,
                    totalPrice
            );

            orderItemContexts.add(new OrderItemContext(product, stock, info.quantity(), totalPrice));
        }

        // 포인트 조회 + 비관락
        Point point = pointService.findPointWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저입니다."));


        // 주문처리 도메인서비스 호출
        orderDomainService.processPaymentAndInventory(order, orderItemContexts, point);

        // 주문 저장
        orderService.createOrder(order);

        // 주문 정보 외부 시스템 전송 (실패해도 주문 트랜잭션은 롤백하지 않도록 catch)
        try {
            orderExternalSystemSender.send(order);
        } catch (Exception e) {
            log.error("외부 시스템으로의 주문 전송 실패. 주문 ID: {}", order.getId(), e);
        }

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long userId, Long orderId) {
        Order order = orderService.findOrder(userId, orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long userId) {
        List<Order> orders = orderService.findAll(userId);
        return orders;
    }
}
