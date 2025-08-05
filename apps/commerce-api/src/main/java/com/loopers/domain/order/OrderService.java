package com.loopers.domain.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductOptionModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderService {

    public OrderModel createOrder(Long userId) {
        return OrderModel.register(userId);
    }

    public void addOrderItem(OrderModel order, Long productId, Long optionId, 
                           BigDecimal quantity, BigDecimal price,
                           String productName, String optionName, String imageUrl) {
        order.addItem(productId, optionId, quantity, price, productName, optionName, imageUrl);
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
