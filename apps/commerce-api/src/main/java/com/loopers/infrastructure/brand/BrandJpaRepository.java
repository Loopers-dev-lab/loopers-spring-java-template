package com.loopers.infrastructure.brand;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 브랜드 JPA Repository
 */
public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
}
