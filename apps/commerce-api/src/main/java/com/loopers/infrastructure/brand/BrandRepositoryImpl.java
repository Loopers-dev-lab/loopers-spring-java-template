package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class BrandRepositoryImpl implements BrandRepository {
    
    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id);
    }

    @Override
    public List<Brand> findByIdIn(List<Long> ids) {
        return brandJpaRepository.findAllById(ids);
    }

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }
}