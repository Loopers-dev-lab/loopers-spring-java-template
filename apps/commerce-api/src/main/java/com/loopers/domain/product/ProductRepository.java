package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAllByIds(Collection<Long> ids); // OrderService에서 사용
    Page<Product> findProducts(Pageable pageable, Long brandId); // 정렬 조건
}
