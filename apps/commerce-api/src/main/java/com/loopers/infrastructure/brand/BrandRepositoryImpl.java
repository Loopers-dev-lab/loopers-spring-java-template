package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * packageName : com.loopers.infrastructure.brand
 * fileName     : BrandRepositroyImpl
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository jpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void save(Brand brand) {
        jpaRepository.save(brand);
    }
}
