package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    Optional<Brand> findById(Long id);

    Collection<Brand> findAllByIdIn(Collection<Long> ids);

    Brand save(Brand brand);

    Page<Brand> findAll(Pageable pageable);

    List<Brand> saveAll(Collection<Brand> brands);
}
