package com.loopers.domain.brand;

import java.util.Optional;

/**
 * packageName : com.loopers.domain.brand
 * fileName     : BrandRepository
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
public interface BrandRepository {
    Optional<Brand> findById(Long id);

    void save(Brand brand);
}
