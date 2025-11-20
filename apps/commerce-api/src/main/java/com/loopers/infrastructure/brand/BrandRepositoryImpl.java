package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(final Long brandId) {
        return brandJpaRepository.findById(brandId);
    }

    @Override
    public List<Brand> findAllById(final Set<Long> brandIds) {
        return brandJpaRepository.findAllById(brandIds);
    }
}
