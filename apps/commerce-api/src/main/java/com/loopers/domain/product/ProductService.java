package com.loopers.domain.product;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;

  public Optional<Product> getById(Long productId) {
    return productRepository.findById(productId);
  }

  @Transactional
  public List<Product> findByIdsWithLock(List<Long> productIds) {
    Objects.requireNonNull(productIds, "상품 ID 목록은 null일 수 없습니다");
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findAllByIdWithLock(distinctIds);
  }

  public List<Product> findByIdIn(List<Long> productIds) {
    Objects.requireNonNull(productIds, "상품 ID 목록은 null일 수 없습니다");
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findByIdIn(distinctIds);
  }

  public Page<Product> findProducts(Long brandId, Pageable pageable) {
    return brandId != null
        ? productRepository.findByBrandId(brandId, pageable)
        : productRepository.findAll(pageable);
  }

  public Product create(Product product) {
    return productRepository.saveAndFlush(product);
  }

  @Transactional
  public void increaseLikeCount(Long productId) {
    productRepository.incrementLikeCount(productId);
  }

  @Transactional
  public void decreaseLikeCount(Long productId) {
    productRepository.decrementLikeCount(productId);

  }

}
