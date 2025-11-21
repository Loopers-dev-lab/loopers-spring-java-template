package com.loopers.domain.order;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PointService pointService;

    @Transactional(readOnly = true)
    public OrderModel getOrder(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true) 
    public List<OrderModel> getUserOrders(UserModel user) {
        return orderRepository.findByUserId(user);
    }

    @Transactional
    public OrderModel createOrder(UserModel user, List<OrderItemRequest> items) {

        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.");
        }

        List<OrderItemModel> orderItems = new ArrayList<>();
        long totalPriceValue = 0;

        // 각 상품에 대해 재고 확인 및 차감, 주문 항목 생성
        for (OrderItemRequest item : items) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1개 이상이어야 합니다.");
            }
            
            ProductModel product = productService.getProduct(item.productId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다. productId: " + item.productId());
            }

            Quantity quantity = new Quantity(item.quantity());
            
            // 재고 차감
            productService.updateQuantity(item.productId(), quantity);
            
            // 주문 항목 가격 계산 (상품 가격 * 수량)
            Money orderPrice = new Money(product.getPrice().value() * quantity.quantity());
            totalPriceValue += orderPrice.value();
            
            // 주문 항목 생성
            OrderItemModel orderItem = new OrderItemModel(product, quantity, orderPrice);
            orderItems.add(orderItem);
        }

        Money totalPrice = new Money(totalPriceValue);
        
        // 포인트 차감
        pointService.use(user, totalPrice);
        
        // 주문 생성 및 저장
        OrderModel order = new OrderModel(user, totalPrice, orderItems);
        return orderRepository.save(order);
    }

    public record OrderItemRequest(Long productId, Integer quantity) {}
}
