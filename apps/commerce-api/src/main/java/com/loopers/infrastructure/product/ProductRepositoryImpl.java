package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {
  private final ProductJpaRepository jpaRepository;

  @Override
  public Optional<Product> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public List<Product> findAllById(Set<Long> id) {
    return jpaRepository.findAllById(id);
  }

  @Override
  public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
    return jpaRepository.findByBrandId(brandId, pageable);
  }

  @Override
  public Page<Product> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable);
  }

  @Override
  public Product save(Product product) {
    return jpaRepository.save(product);
  }

  @Override
  public List<Product> saveAll(List<Product> products) {
    return jpaRepository.saveAll(products);
  }
}
