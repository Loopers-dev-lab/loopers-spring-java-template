package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    BrandModel save(BrandModel brandModel);
    Optional<BrandModel> findById(Long id);

    List<BrandModel> findByBrandIds(List<Long> brandIds);

    void deleteAll();
}
