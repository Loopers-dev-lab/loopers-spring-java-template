package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandJpaRepository extends JpaRepository<BrandModel, Long> {
    boolean existsByName(String brandName);
    Optional<BrandModel> findByName(String brandName);
    List<BrandModel> findAllByStatus(BrandStatus brandStatus);
    boolean deleteBrand(Long id);
}
