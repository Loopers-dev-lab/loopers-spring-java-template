package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id);
    }

    @Override
    public Collection<Brand> findAllByIdIn(Collection<Long> ids) {
        return brandJpaRepository.findAllById(ids);
    }

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }

    @Override
    public Page<Brand> findAll(Pageable pageable) {
        return brandJpaRepository.findAll(pageable);
    }

    @Override
    public List<Brand> saveAll(Collection<Brand> brands) {
        return brandJpaRepository.saveAll(brands);
    }
}
