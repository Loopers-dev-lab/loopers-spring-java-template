package com.loopers.domain.product;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;

  public Optional<Product> getById(Long productId) {
    return productRepository.findById(productId);
  }

  @Transactional
  public List<Product> getByIdsWithLock(List<Long> productIds) {
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findAllByIdWithLock(distinctIds);
  }

  public Page<Product> findProducts(Long brandId, Pageable pageable) {
    return brandId != null
        ? productRepository.findByBrandId(brandId, pageable)
        : productRepository.findAll(pageable);
  }

}
