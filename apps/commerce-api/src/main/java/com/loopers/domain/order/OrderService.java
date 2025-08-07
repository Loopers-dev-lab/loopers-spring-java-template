package com.loopers.domain.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.domain.order.item.OrderItemModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductOptionModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class OrderService {

    public OrderModel createOrder(Long userId) {
        return OrderModel.register(userId);
    }

    public OrderModel createOrderWithRetry(Long userId, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return OrderModel.register(userId);
            } catch (DataIntegrityViolationException e) {
                if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("order_number")) {
                    if (attempt == maxRetries - 1) {
                        throw new CoreException(ErrorType.INTERNAL_ERROR,
                            "주문번호 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
                    }
                    try {
                        Thread.sleep(1 + (int) (Math.random() * 4));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CoreException(ErrorType.INTERNAL_ERROR, "주문 처리가 중단되었습니다.");
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new CoreException(ErrorType.INTERNAL_ERROR, "주문 생성에 실패했습니다.");
    }
    
    public OrderModel createOrderWithRetry(Long userId) {
        return createOrderWithRetry(userId, 3);
    }

    public OrderItemModel createOrderItem(OrderModel order, Long productId, Long optionId,
                                          int quantity, BigDecimal price,
                                          String productName, String optionName, String imageUrl) {
        return OrderItemModel.of(order.getId(), productId, optionId, quantity, price, productName, optionName, imageUrl);
    }
    public BigDecimal calculatePrice(ProductModel product, ProductOptionModel option, int quantity) {
        BigDecimal base = product.getPrice().getValue();
        BigDecimal totalOptionPrice = option.calculateTotalPrice(base);
        return totalOptionPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void validateOrderItem(int quantity, ProductModel product, ProductOptionModel option) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }
        if (!option.getProductId().getValue().equals(product.getId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품에 해당 옵션이 아닙니다.");
        }
    }
    @Transactional()
    public OrderCommand.OrderItemData processOrderItem(int quantity, ProductModel product, ProductOptionModel option) {
        validateOrderItem(quantity, product, option);
        
        BigDecimal totalPrice = calculatePrice(product, option, quantity);
        
        return OrderCommand.OrderItemData.of(
            product.getId(),
            option.getId(),
            quantity,
            totalPrice,
            product.getProductName().getValue(),
            option.getName().getValue(),
            product.getImgUrl().getValue()
        );
    }

    
}
