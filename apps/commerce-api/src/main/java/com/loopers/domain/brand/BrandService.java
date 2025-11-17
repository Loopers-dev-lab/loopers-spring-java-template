package com.loopers.domain.brand;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BrandService {

  private final BrandRepository brandRepository;


  public Optional<Brand> getById(Long brandId) {
    return brandRepository.findById(brandId);
  }


  public List<Brand> findByIdIn(List<Long> brandIds) {
    return brandRepository.findByIdIn(brandIds);
  }
}
