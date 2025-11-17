package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
  
  Optional<Brand> findById(Long id);

  List<Brand> findByIdIn(List<Long> ids);

  Brand save(Brand brand);
}
