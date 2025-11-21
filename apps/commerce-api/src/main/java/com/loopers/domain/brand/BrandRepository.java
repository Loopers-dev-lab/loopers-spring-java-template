package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
  Optional<Brand> findById(Long id);

  Brand save(Brand brand);

  List<Brand> saveAll(List<Brand> brand);
}
