package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BrandRepository {
    Optional<Brand> findById(Long brandId);

    List<Brand> findAllById(Set<Long> brandIds);
}
