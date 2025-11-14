package com.loopers.domain.brand;

import java.util.Optional;

public interface BrandRepository {
  Optional<Brand> findById(Long id);

  Brand save(Brand brand);

}
