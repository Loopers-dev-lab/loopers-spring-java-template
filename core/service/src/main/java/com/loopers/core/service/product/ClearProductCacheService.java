package com.loopers.core.service.product;

import com.loopers.core.domain.product.repository.ProductCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.ClearProductCacheCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClearProductCacheService {

    private final ProductCacheRepository productCacheRepository;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "CLEAR_PRODUCT_CACHE",
            eventIdField = "eventId",
            aggregateIdField = "id"
    )
    public void clear(ClearProductCacheCommand command) {
        productCacheRepository.delete(new ProductId(command.productId()));
    }
}
