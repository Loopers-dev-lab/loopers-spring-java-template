package com.loopers.application.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Products;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class OrderItemFactory {

  public OrderItem create(OrderItemCommand command, Products products) {
    Product product = products.findById(command.productId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    return OrderItem.of(
        product.getId(),
        product.getName(),
        command.quantity(),
        OrderPrice.of(product.getPriceValue())
    );
  }
}