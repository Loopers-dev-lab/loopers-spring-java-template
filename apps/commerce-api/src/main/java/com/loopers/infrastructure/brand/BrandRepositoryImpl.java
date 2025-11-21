package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {
  private final BrandJpaRepository jpaRepository;

  @Override
  public Optional<Brand> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Brand save(Brand brand) {
    return jpaRepository.save(brand);
  }

  @Override
  public List<Brand> saveAll(List<Brand> brands) {
    return jpaRepository.saveAll(brands);
  }
}
