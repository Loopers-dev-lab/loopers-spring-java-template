package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

  Page<Product> findAll(Pageable pageable);

  Page<Product> findByBrandId(Long brandId, Pageable pageable);

  Optional<Product> findById(Long id);

  List<Product> findAllById(List<Long> ids);

  List<Product> findAllByIdWithLock(List<Long> ids);

  Product save(Product product);
}
