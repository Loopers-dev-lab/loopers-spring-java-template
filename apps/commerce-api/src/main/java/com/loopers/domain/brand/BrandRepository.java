package com.loopers.domain.brand;

import java.util.Optional;

public interface BrandRepository {
    Brand save(Brand brand);

    Optional<Brand> findById(Long id);

    Optional<Brand> findByName(String name);

    boolean existsByName(String name);
}
