package com.loopers.domain.order;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final PointService pointService;

    /**
     * 주문 생성 및 처리
     * 1. 주문 아이템 생성 및 검증
     * 2. 주문 생성 (PENDING 상태)
     * 3. 재고 차감
     * 4. Payment 생성 및 계산
     * 5. 포인트 차감 (Payment를 통해)
     * 6. 주문 저장 및 상태 변경
     */
    public Order createOrder(User user, List<OrderItemRequest> orderItemRequests) {
        // 1. 주문 아이템 생성 및 검증
        List<OrderItem> orderItems = orderItemRequests.stream()
                .map(request -> {
                    Product product = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new CoreException(
                                    ErrorType.NOT_FOUND,
                                    "[productId = " + request.getProductId() + "] Product를 찾을 수 없습니다."
                            ));

                    // 상품 판매 가능 여부 확인
                    if (!product.getIsSellable()) {
                        throw new CoreException(
                                ErrorType.BAD_REQUEST,
                                "[productId = " + request.getProductId() + "] 판매 불가능한 상품입니다."
                        );
                    }

                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

                    return OrderItem.builder()
                            .quantity(request.getQuantity())
                            .unitPrice(unitPrice)
                            .totalAmount(totalAmount)
                            .product(product)
                            .build();
                })
                .toList();

        // 2. 주문 금액 계산
        BigDecimal totalPrice = orderItems.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO; // 할인 로직은 추후 구현
        BigDecimal shippingFee = BigDecimal.ZERO; // 배송비 로직은 추후 구현

        // 3. 주문 생성 (PENDING 상태, finalAmount는 임시로 0 설정)
        Order order = Order.builder()
                .finalAmount(BigDecimal.ZERO) // Payment에서 계산된 값으로 업데이트됨
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .orderStatus(OrderStatus.PENDING)
                .user(user)
                .orderItems(orderItems)
                .build();

        // 4. 재고 차감 (재고 부족 시 예외 발생)
        for (OrderItem orderItem : orderItems) {
            stockService.decreaseQuantity(
                    orderItem.getProduct().getId(),
                    Long.valueOf(orderItem.getQuantity())
            );
        }

        // 5. Payment 생성 및 계산
        Payment payment = Payment.create(
                order,
                totalPrice,
                discountAmount,
                shippingFee,
                PaymentMethod.POINT
        );

        // Payment에서 계산된 최종 금액을 Order에 적용
        BigDecimal finalAmount = payment.getAmount();
        order.setFinalAmount(finalAmount);

        // 6. 포인트 조회
        Point point = pointService.findByUserLoginId(user.getLoginId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + user.getLoginId() + "] Point를 찾을 수 없습니다."
                ));

        // 7. Payment를 통해 포인트 차감 처리 (포인트 부족 시 예외 발생)
        payment.processWithPoint(point);

        // 8. 주문 저장
        Order savedOrder = orderRepository.save(order)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "주문 저장에 실패했습니다."
                ));

        // 9. 주문 상태를 CONFIRMED로 변경
        savedOrder.confirm();
        
        return savedOrder;
    }

    /**
     * 주문 조회
     */
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[orderId = " + orderId + "] Order를 찾을 수 없습니다."
                ));
    }

    /**
     * 유저의 주문 목록 조회
     */
    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }

    /**
     * 주문 아이템 요청 DTO
     */
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }
}

