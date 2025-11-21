package com.loopers.domain.product.dto;

import org.springframework.data.domain.Pageable;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public record ProductSearchFilter(
        Long brandId,
        String productName,
        Pageable pageable
) {
}
