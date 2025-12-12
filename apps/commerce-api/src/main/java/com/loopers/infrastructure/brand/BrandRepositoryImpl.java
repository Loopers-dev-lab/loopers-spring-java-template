package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 브랜드 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Brand save(Brand brand) {
        BrandEntity entity;

        // ID가 있으면 기존 엔티티 업데이트, 없으면 새로 생성
        if (brand.getId() != null) {
            entity = brandJpaRepository.findById(brand.getId())
                    .orElseThrow(() -> new IllegalStateException("존재하지 않는 브랜드입니다: " + brand.getId()));
            entity.updateFrom(brand);
        } else {
            entity = BrandEntity.from(brand);
        }

        BrandEntity saved = brandJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id)
                .map(BrandEntity::toDomain);
    }

    @Override
    public List<Brand> findAll() {
        return brandJpaRepository.findAll().stream()
                .map(BrandEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        return brandJpaRepository.existsById(id);
    }
}
