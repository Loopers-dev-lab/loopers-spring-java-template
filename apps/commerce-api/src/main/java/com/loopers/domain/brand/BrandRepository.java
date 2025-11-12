package com.loopers.domain.brand;

import com.loopers.domain.user.UserModel;

import java.util.Optional;

public interface BrandRepository {
    boolean existsById(Long id);
    boolean existsByName(String brandName);
    Optional<BrandModel> findById(Long id);
    Optional<BrandModel> findByName(String brandName);

    BrandModel save(BrandModel brandModel);
    boolean deleteBrand(Long id);
}
