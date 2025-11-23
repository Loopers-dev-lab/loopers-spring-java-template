package com.loopers.application.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;

// orderItem + product + stock
public record OrderItemContext(
        Product product,
        Stock stock,
        int orderQuantity,
        Money orderPrice
) {}
