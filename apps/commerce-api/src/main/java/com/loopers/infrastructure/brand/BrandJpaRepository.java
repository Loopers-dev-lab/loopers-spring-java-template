package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {
    
    @Query("SELECT b FROM Brand b WHERE b.id IN :brandIds AND b.deletedAt IS NULL")
    List<Brand> findByIds(@Param("brandIds") List<Long> brandIds);

    @Query("SELECT b.name FROM Brand b WHERE b.id = :brandId AND b.deletedAt IS NULL")
    String findNameById(@Param("brandId") Long brandId);
}

