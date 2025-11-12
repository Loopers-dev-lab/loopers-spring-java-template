package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

  private final BrandRepository brandRepository;


  public Brand getById(Long brandId) {
    return brandRepository.findById(brandId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
  }


  public List<Brand> findByIdIn(List<Long> brandIds) {
    return brandRepository.findByIdIn(brandIds);
  }
}
