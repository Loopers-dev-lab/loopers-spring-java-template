package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;

  public Product getById(Long productId) {
    return productRepository.findById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
  }

  public List<Product> getByIds(List<Long> productIds) {
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findAllById(distinctIds);
  }

  public List<Product> getByIdsWithLock(List<Long> productIds) {
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findAllByIdWithLock(distinctIds);
  }

  public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
    return productRepository.findByBrandId(brandId, pageable);
  }

  public Page<Product> findAll(Pageable pageable) {
    return productRepository.findAll(pageable);
  }
}
