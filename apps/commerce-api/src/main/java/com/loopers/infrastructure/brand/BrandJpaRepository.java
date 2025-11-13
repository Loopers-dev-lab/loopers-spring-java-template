package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Brand 엔티티를 위한 Spring Data JPA 리포지토리.
 */
public interface BrandJpaRepository extends JpaRepository<Brand, Long> {
}

