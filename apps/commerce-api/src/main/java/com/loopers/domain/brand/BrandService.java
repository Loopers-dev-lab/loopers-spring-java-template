package com.loopers.domain.brand;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
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
