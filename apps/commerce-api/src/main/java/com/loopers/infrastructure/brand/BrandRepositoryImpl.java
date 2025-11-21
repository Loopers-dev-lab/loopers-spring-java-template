package com.loopers.infrastructure.brand;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.dto.BrandSearchFilter;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
@Component
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;
    private final BrandQueryRepository queryRepository;

    @Override
    public BrandEntity save(BrandEntity brand) {
        return brandJpaRepository.save(brand);
    }

    @Override
    public Page<BrandEntity> listBrands(Pageable pageable) {
        return searchBrands(BrandSearchFilter.of(null), pageable);
    }

    @Override
    public Optional<BrandEntity> getBrandById(long id) {
        return brandJpaRepository.findByIdAndDeletedAtNull(id);
    }

    @Override
    public Optional<BrandEntity> findByName(String name) {
        return brandJpaRepository.findByNameAndDeletedAtNull(name);
    }

    @Override
    public Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable) {
        return queryRepository.searchBrands(filter, pageable);
    }
}
