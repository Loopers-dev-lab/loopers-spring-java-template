package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

  private final ProductJpaRepository jpaRepository;

  @Override
  public Page<Product> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable);
  }

  @Override
  public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
    return jpaRepository.findByBrandId(brandId, pageable);
  }

  @Override
  public Optional<Product> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public List<Product> findAllById(List<Long> ids) {
    return jpaRepository.findAllById(ids);
  }

  @Override
  public List<Product> findAllByIdWithLock(List<Long> ids) {
    return jpaRepository.findAllByIdWithLock(ids);
  }

  @Override
  public Product save(Product product) {
    return jpaRepository.save(product);
  }
}
