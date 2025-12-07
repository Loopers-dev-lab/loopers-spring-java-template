package com.loopers.support.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CacheKeyService {
    public String productListKey(Long brandId, Pageable pageable, String sort) {
        return String.format(
                "product:v1:list:%s:%d:%d:%s",
                brandId == null ? "all" : brandId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    public String productDetailKey(Long productId) {
        return "product:v1:detail:" + productId;
    }
}
