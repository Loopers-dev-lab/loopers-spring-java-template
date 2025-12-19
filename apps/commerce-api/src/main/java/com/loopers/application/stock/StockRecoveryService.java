package com.loopers.application.stock;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.infrastructure.cache.ProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRecoveryService {

    private final ProductDomainService productDomainService;
    private final ProductCacheService productCacheService;

    public void recover(Order order) {
        List<OrderItem> items = new ArrayList<>(order.getOrderItems());
        items.sort(Comparator.comparing(OrderItem::getProductId).reversed());

        for (OrderItem item : items) {
            productDomainService.increaseStock(item.getProductId(), item.getQuantity());
            productCacheService.deleteProductDetail(item.getProductId());
        }

        log.info("Stock recovered for orderId: {}, itemCount: {}", order.getId(), items.size());
    }
}
