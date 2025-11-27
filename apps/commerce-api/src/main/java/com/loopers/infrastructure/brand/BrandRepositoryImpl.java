package com.loopers.infrastructure.brand;

import com.loopers.cache.RedisCacheTemplate;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.infrastructure.cache.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

  private final BrandJpaRepository jpaRepository;
  private final RedisCacheTemplate cacheTemplate;

  @Override
  public Optional<Brand> findById(Long id) {
    Brand brand = cacheTemplate.getOrLoad(
        CacheKeys.brand(id),
        () -> jpaRepository.findById(id).orElse(null)
    );
    return Optional.ofNullable(brand);
  }

  @Override
  public List<Brand> findByIdIn(List<Long> ids) {
    return jpaRepository.findByIdIn(ids);
  }

  @Override
  public Brand save(Brand brand) {
    return jpaRepository.save(brand);
  }
}
