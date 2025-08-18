package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandModel, Long> {
    List<BrandModel> findByIdIn(List<Long> brandIds);
}
