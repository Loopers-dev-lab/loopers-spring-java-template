package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByIdIn(Set<Long> ids);

    @Query("SELECT b FROM Brand b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Brand> findByIdAndNotDeleted(@Param("id") Long id);
}
