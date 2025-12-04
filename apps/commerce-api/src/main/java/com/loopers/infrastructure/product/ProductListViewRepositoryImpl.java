package com.loopers.infrastructure.product;

import com.loopers.domain.view.ProductListView;
import com.loopers.domain.view.ProductListViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductListViewRepositoryImpl implements ProductListViewRepository {
  private final ProductListViewJpaRepository jpaRepository;

  @Override
  public Page<ProductListView> findByBrandId(Long brandId, Pageable pageable) {
    return jpaRepository.findByBrandId(brandId, pageable);
  }

  @Override
  public Page<ProductListView> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable);
  }

  @Override
  public Optional<ProductListView> getProductListView(Long id) {
    return jpaRepository.findByProductId(id);
  }

  @Override
  public List<ProductListView> getProductListViews(List<Long> id) {
    return jpaRepository.findByProductIds(id);
  }

  @Override
  public ProductListView save(ProductListView productListView) {
    return jpaRepository.save(productListView);
  }

  @Override
  public List<ProductListView> saveAll(List<ProductListView> productListView) {
    return jpaRepository.saveAll(productListView);
  }

}
