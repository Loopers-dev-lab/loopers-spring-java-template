package com.loopers.infrastructure.brand;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.brand.BrandEntity;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
    Page<BrandEntity> findAllByDeletedAtNull(Pageable pageable);

    Optional<BrandEntity> findByIdAndDeletedAtNull(long id);

    Optional<BrandEntity> findByNameAndDeletedAtNull(String name);
}
