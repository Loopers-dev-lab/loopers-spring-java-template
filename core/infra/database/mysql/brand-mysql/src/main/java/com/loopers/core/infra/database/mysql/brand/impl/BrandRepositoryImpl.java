package com.loopers.core.infra.database.mysql.brand.impl;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.infra.database.mysql.brand.BrandJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository repository;

    @Override
    public Brand getBrandById(BrandId brandId) {
        return repository.findById(
                        Objects.requireNonNull(Optional.ofNullable(brandId.value())
                                .map(Long::parseLong)
                                .orElse(null))
                ).orElseThrow(() -> NotFoundException.withName("브랜드"))
                .to();
    }
}
