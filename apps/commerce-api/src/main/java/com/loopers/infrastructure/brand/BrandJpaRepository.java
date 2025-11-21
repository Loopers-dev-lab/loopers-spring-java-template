package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * packageName : com.loopers.infrastructure.brand
 * fileName     : BrandJpaRepository
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
public interface BrandJpaRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findById(Long id);
}
