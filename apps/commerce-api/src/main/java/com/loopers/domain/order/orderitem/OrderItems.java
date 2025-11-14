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

  public Long calculateTotalAmount() {
    return items.stream()
        .mapToLong(item -> item.getOrderPriceValue() * item.getQuantityValue())
        .sum();
  }

  public void validateStock(Products products) {
    for (OrderItem item : items) {
      Product product = findById(products, item);
      product.validateStockForOrder(item.getQuantityValue());
    }
  }

  public void decreaseStock(Products products) {
    for (OrderItem item : items) {
      Product product = findById(products, item);
      product.decreaseStock(item.getQuantityValue().longValue());
    }
  }

  private Product findById(Products products, OrderItem item) {
    return products.findById(item.getProductId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
  }

  public List<OrderItem> getItems() {
    return List.copyOf(items);
  }
}
