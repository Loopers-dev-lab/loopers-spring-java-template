package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    boolean existsById(Long id);
    boolean existsByName(String brandName);
    Optional<BrandModel> findById(Long id);
    Optional<BrandModel> findByName(String brandName);
    List<BrandModel> findAllByStatus(BrandStatus brandStatus);
    BrandModel save(BrandModel brandModel);
    boolean disContinueBrandById(Long id);
    boolean disContinueBrandByName(String name);
}
