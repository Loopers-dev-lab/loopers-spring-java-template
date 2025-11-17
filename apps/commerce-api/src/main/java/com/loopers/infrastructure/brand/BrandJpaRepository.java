package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {

  List<Brand> findByIdIn(List<Long> ids);
}
