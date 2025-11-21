package com.loopers.application.product;

import org.springframework.data.domain.Pageable;

public record ProductGetListCommand(
        Long brandId,
        Pageable pageable
) {
}
