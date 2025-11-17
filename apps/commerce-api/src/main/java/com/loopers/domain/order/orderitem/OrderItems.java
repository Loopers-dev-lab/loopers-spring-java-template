package com.loopers.domain.order.orderitem;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderItems {

  private final List<OrderItem> items;

  public OrderItems(List<OrderItem> items) {
    this.items = items;
  }

  public Long calculateTotalAmount() {
    return items.stream()
        .mapToLong(item -> item.getOrderPriceValue() * item.getQuantityValue())
        .sum();
  }

  public void validateStock(Map<Long, Product> productById) {
    for (OrderItem item : items) {
      Product product = findById(productById, item);
      product.validateStockForOrder(item.getQuantityValue());
    }
  }

  public void decreaseStock(Map<Long, Product> productById) {
    for (OrderItem item : items) {
      Product product = findById(productById, item);
      product.decreaseStock(item.getQuantityValue());
    }
  }

  private Product findById(Map<Long, Product> productById, OrderItem item) {
    return Optional.ofNullable(productById.get(item.getProductId()))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
  }

  public List<OrderItem> getItems() {
    return List.copyOf(items);
  }
}
