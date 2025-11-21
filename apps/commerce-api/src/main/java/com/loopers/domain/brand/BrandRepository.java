package com.loopers.domain.brand;

import java.util.Collection;
import java.util.Optional;

public interface BrandRepository {
    Optional<Brand> findById(Long id);

    Collection<Brand> findAllByIdIn(Collection<Long> ids);
}
