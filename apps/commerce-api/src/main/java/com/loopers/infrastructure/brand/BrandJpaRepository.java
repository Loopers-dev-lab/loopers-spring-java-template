package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<Brand, Long>, BrandRepository {

    @Override
    Optional<Brand> findByName(String name);

    @Override
    boolean existsByName(String name);
}
