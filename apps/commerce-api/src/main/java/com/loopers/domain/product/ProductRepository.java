package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository {
  Optional<Product> findById(Long id);

  List<Product> findAllById(Set<Long> id);

  Page<Product> findByBrandId(Long brandId, Pageable pageable);

  Page<Product> findAll(Pageable pageable);

  Product save(Product product);

  List<Product> save(List<Product> products);

}
