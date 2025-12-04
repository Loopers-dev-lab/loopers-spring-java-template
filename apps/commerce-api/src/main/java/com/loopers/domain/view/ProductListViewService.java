package com.loopers.domain.view;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductListViewService {

  private final ProductListViewRepository productListViewRepository;

  @Transactional(readOnly = true)
  public Page<ProductListView> getProducts(Long brandId, String sortType, int page, int size) {
    Sort sort = this.getSortBySortType(sortType);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ProductListView> products = null;
    if (brandId != null) {
      products = productListViewRepository.findByBrandId(brandId, pageable);
    } else {
      products = productListViewRepository.findAll(pageable);
    }
    return products;
  }

  @Transactional(readOnly = true)
  public List<ProductListView> getByProductIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "조회할 ID 목록이 없습니다.");
    }

    return productListViewRepository.getProductListViews(ids);
  }

  @Transactional(readOnly = true)
  public ProductListView getByProductId(Long id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    Optional<ProductListView> product = productListViewRepository.getProductListView(id);
    if (product.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
    }
    return product.get();
  }

  @Transactional
  public ProductListView save(ProductListView productListView) {
    return productListViewRepository.save(productListView);
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
