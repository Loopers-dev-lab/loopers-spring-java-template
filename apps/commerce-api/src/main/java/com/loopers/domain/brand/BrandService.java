package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandService {

  private final BrandRepository brandRepository;

  @Transactional(readOnly = true)
  public Brand getExistingBrand(Long brandId) {
    return brandRepository.findById(brandId)
        .orElseThrow(() -> new IllegalArgumentException("브랜드가 존재하지 않습니다. id=" + brandId));
  }

  @Transactional(readOnly = true)
  public Brand getBrand(Long id) {
    if (id == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return brandRepository.findById(id).orElse(null);
  }

  @Transactional
  public Brand save(Brand brand) {
    return brandRepository.save(brand);
  }

  @Transactional
  public List<Brand> saveAll(List<Brand> brands) {
    return brandRepository.saveAll(brands);
  }

}
