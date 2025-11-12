package com.loopers.domain.order.orderitem;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.Products;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;


public class OrderItems {

  private final List<OrderItem> items;

  public OrderItems(List<OrderItem> items) {
    this.items = items;
  }

  public static OrderItems from(List<OrderItem> items) {
    return new OrderItems(items);
  }


  public Long calculateTotalAmount() {
    return items.stream()
        .mapToLong(item -> item.getOrderPriceValue() * item.getQuantityValue())
        .sum();
  }

  public List<Long> getProductIds() {
    return items.stream()
        .map(OrderItem::getProductId)
        .toList();
  }

  public void validateStock(Products products) {
    for (OrderItem item : items) {
      Product product = products.findById(item.getProductId())
          .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
      product.validateStockForOrder(item.getQuantityValue());
    }
  }

  public void decreaseStock(Products products) {
    for (OrderItem item : items) {
      Product product = products.findById(item.getProductId())
          .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
      product.decreaseStock(item.getQuantityValue().longValue());
    }
  }

  public List<OrderItem> getItems() {
    return List.copyOf(items);
  }
}
