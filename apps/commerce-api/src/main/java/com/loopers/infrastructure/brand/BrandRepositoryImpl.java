package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id);
    }

    @Override
    public List<Brand> findByIdIn(Set<Long> ids) {
        return brandJpaRepository.findByIdIn(ids);
    }

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }
}
