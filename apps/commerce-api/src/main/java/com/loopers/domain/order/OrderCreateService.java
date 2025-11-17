package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.orderitem.OrderItems;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OrderCreateService {

  public OrderPreparation prepareOrder(List<OrderItemCommand> commands,
                                          Map<Long, Product> productById) {
    OrderItems orderItems = createOrderItems(commands, productById);

    orderItems.validateStock(productById);

    Long totalAmount = orderItems.calculateTotalAmount();

    return new OrderPreparation(orderItems, totalAmount);
  }

  private OrderItems createOrderItems(List<OrderItemCommand> commands,
                                      Map<Long, Product> productById) {
    List<OrderItem> items = commands.stream()
        .map(cmd -> createOrderItem(cmd, productById))
        .toList();

    return new OrderItems(items);
  }

  private OrderItem createOrderItem(OrderItemCommand command, Map<Long, Product> productById) {
    Product product = Optional.ofNullable(productById.get(command.productId()))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    return OrderItem.of(
        product.getId(),
        product.getName(),
        command.quantity(),
        OrderPrice.of(product.getPriceValue())
    );
  }
}
