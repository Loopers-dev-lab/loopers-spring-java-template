package com.loopers.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


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

    public String rankingKey(LocalDate date) {
        return "ranking:all:" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
