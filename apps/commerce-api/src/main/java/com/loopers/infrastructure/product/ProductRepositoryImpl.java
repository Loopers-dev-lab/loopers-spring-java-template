package com.loopers.infrastructure.product;

import com.loopers.cache.CacheTemplate;
import com.loopers.cache.SimpleCacheKey;
import com.loopers.domain.cache.CachePolicy;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

  private final ProductJpaRepository jpaRepository;
  private final CacheTemplate cacheTemplate;

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
    Product product = cacheTemplate.getOrLoad(
        SimpleCacheKey.of(CachePolicy.PRODUCT.buildKey(id), CachePolicy.PRODUCT.getTtl(), Product.class),
        () -> jpaRepository.findById(id).orElse(null)
    );
    return Optional.ofNullable(product);
  }

  @Override
  public List<Product> findAllByIdWithLock(List<Long> ids) {
    return jpaRepository.findAllByIdWithLock(ids);
  }

  @Override
  public Product save(Product product) {
    return jpaRepository.save(product);
  }

  @Override
  public Product saveAndFlush(Product product) {
    return jpaRepository.saveAndFlush(product);
  }

  @Override
  public void incrementLikeCount(Long productId) {
    jpaRepository.incrementLikeCount(productId);
    cacheTemplate.evict(SimpleCacheKey.of(CachePolicy.PRODUCT.buildKey(productId), CachePolicy.PRODUCT.getTtl(), Product.class));
  }

  @Override
  public void decrementLikeCount(Long productId) {
    jpaRepository.decrementLikeCount(productId);
    cacheTemplate.evict(SimpleCacheKey.of(CachePolicy.PRODUCT.buildKey(productId), CachePolicy.PRODUCT.getTtl(), Product.class));
  }

  @Override
  public void decrementStock(Long productId, Long amount) {
    jpaRepository.decrementStock(productId, amount);
    cacheTemplate.evict(SimpleCacheKey.of(CachePolicy.PRODUCT.buildKey(productId), CachePolicy.PRODUCT.getTtl(), Product.class));
  }
}
