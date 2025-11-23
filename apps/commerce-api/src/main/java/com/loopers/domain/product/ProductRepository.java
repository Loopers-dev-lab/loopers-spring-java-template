package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Page<Product> findProducts(Long brandId, Pageable pageRequest);

    Optional<Product> findById(Long productId);

    Product save(Product product1);

    List<Product> findAllById(List<Long> productIds);
}
