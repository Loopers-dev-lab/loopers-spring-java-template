package com.loopers.domain.brand;

import java.util.List;
import java.util.Set;

public interface BrandRepository {

    List<Brand> findByIdIn(Set<Long> ids);

    Brand save(Brand brand);
}
