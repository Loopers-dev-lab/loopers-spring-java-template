package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long productId);

    Page<Product> findAll(Pageable pageable);

    List<Product> findAllByIdIn(Collection<Long> ids);

    boolean existsById(Long productId);
}
