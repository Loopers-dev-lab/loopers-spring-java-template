package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

  private final BrandJpaRepository jpaRepository;

  @Override
  public Optional<Brand> findById(Long id) {
    return jpaRepository.findById(id);
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
