package com.loopers.application.order;

import com.loopers.application.orderitem.OrderItemInfo;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.orderitem.OrderItem;
import com.loopers.domain.orderitem.OrderItemRepository;
import com.loopers.domain.outbox.AggregateType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.outbox.OutboxType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OutboxRepository outBoxRepository;

    @Transactional
    public OrderResultInfo createOrder(OrderV1Dto.OrderRequest request) {
        /*
        - [ ] 유저 검증(존재 여부 확인)
        - [ ] OrderRequest 내에서 OrderItemRequest 목록을 순회하며 상품 검증(존재 여부 확인)
        - [ ] OrderItem 목록 생성
         */

        // 유저 검증
        Long userId = request.userId();
        userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.BAD_REQUEST, "존재하는 유저가 아닙니다.")
        );

        List<OrderV1Dto.OrderItemRequest> orderItemRequests = request.orderItems();

        List<OrderItem> orderItems = orderItemRequests.stream()
                .map(item -> {
                    // 상품 검증
                    Long productId = item.productId();
                    Product product = productRepository.findById(productId).orElseThrow(
                            () -> new CoreException(ErrorType.NOT_FOUND, "존재하는 상품이 아닙니다.")
                    );

                    if (product.getStock() < item.quantity()) {
                        throw new CoreException(ErrorType.BAD_REQUEST, product.getName() + " 상품의 재고가 부족합니다.");
                    }

                    product.decreaseStock(item.quantity());

                    OrderItem orderItem = item.toEntity(
                            null,
                            product.getPrice().multiply(BigDecimal.valueOf(item.quantity()))
                    );
                    return orderItem;

                })
                .toList();

        BigDecimal totalPrice = orderItems.stream()
                .map(OrderItem::getOrderPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long couponId = request.couponId();

        Coupon coupon = couponRepository.findById(couponId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다.")
        );

        if (coupon.getQuantity() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "남은 쿠폰이 존재하지 않습니다.");
        }

        int rate = coupon.getCouponType().getRate();

        totalPrice = totalPrice
                .multiply(BigDecimal.valueOf(100 - rate))
                .divide(BigDecimal.valueOf(100));

        coupon.useCoupon();

        Order order = request.toEntity(totalPrice);
        Order saved = orderRepository.save(order);

        orderItems.forEach(item -> item.assignOrderId(saved.getId()));
        List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

        savedOrderItems.forEach(orderItem -> {
            OutboxEvent outboxEvent = OutboxEvent.of(
                    AggregateType.PRODUCT,
                    orderItem.getProductId(),
                    OutboxType.PRODUCT_SALES
            );

            outBoxRepository.save(outboxEvent);
        });

        List<OrderItemInfo> orderItemInfos = savedOrderItems.stream()
                .map(orderItem -> OrderItemInfo.from(orderItem, orderItem.getOrderPrice()))
                .toList();

        return new OrderResultInfo(OrderInfo.from(saved), orderItemInfos);
    }
}
