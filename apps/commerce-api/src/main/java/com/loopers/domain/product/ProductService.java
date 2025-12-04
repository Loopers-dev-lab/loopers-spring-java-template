
package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductService {

  private final ProductRepository productRepository;

  @Transactional(readOnly = true)
  public Page<Product> getProducts(
      Long brandId,
      String sortType,
      int page,
      int size
  ) {
    Sort sort = this.getSortBySortType(sortType);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Product> products = null;
    if (brandId != null) {
      products = productRepository.findByBrandId(brandId, pageable);
    } else {
      products = productRepository.findAll(pageable);
    }
    return products;
  }

  @Transactional(readOnly = true)
  public Product getProduct(Long id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    Optional<Product> product = productRepository.findById(id);
    return product.orElse(null);
  }

  @Transactional(readOnly = true)
  public Product getExistingProduct(Long id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    Optional<Product> product = productRepository.findById(id);
    if (product.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
    }
    return product.get();
  }

  @Transactional(readOnly = true)
  public List<Product> getExistingProducts(List<Long> productIds) {

    if (productIds == null || productIds.isEmpty()) {
      return Collections.emptyList();
    }
    Set<Long> uniqueIds = new HashSet<>(productIds);
    List<Product> products = productRepository.findAllById(uniqueIds);

    if (products.size() != uniqueIds.size()) {
      Set<Long> foundIds = products.stream()
          .map(Product::getId)
          .collect(Collectors.toSet());

      uniqueIds.removeAll(foundIds);
      throw new CoreException(
          ErrorType.NOT_FOUND,
          "다음 상품 ID들은 찾을 수 없습니다: " + uniqueIds.toString()
      );
    }
    return products;
  }

  @Transactional
  public Product save(Product product) {
    return productRepository.save(product);
  }

  @Transactional
  public List<Product> saveAll(List<Product> product) {
    return productRepository.saveAll(product);
  }

  private Sort getSortBySortType(String sortType) {
    if (sortType == null) sortType = "latest";
    Sort latestSort = Sort.by("createdAt").descending();
    switch (sortType.toLowerCase()) {
      case "latest":
        return latestSort;
      case "price_asc":
        return Sort.by("totalPrice").ascending().and(latestSort);
      case "likes_desc":
        return Sort.by("likeCount").descending().and(latestSort);
      default:
        return latestSort;
    }
  }

}
