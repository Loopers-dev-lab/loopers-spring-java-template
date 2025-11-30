package com.loopers.application.product;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public record ProductListCache(
    List<ProductDetail> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

  public static ProductListCache from(Page<ProductDetail> page) {
    return new ProductListCache(
        List.copyOf(page.getContent()),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
    );
  }

  public Page<ProductDetail> toPage() {
    return new PageImpl<>(
        content,
        PageRequest.of(page, size),
        totalElements
    );
  }
}
