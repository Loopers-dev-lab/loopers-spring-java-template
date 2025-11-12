package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandJpaRepository extends JpaRepository<BrandModel, Long> {
    boolean existsByName(String brandName);
    Optional<BrandModel> findByName(String brandName);
    boolean deleteBrand(Long id);
}
