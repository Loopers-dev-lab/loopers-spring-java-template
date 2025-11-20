package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BrandRepository의 JPA 구현체.
 */
@RequiredArgsConstructor
@Repository
public class BrandRepositoryImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Brand save(Brand brand) {
        return brandJpaRepository.save(brand);
    }

    /**
     * Finds a brand by its identifier.
     *
     * @param brandId the identifier of the brand to locate
     * @return an Optional containing the Brand with the specified id, or empty if none is found
     */
    @Override
    public Optional<Brand> findById(Long brandId) {
        return brandJpaRepository.findById(brandId);
    }

    /**
     * Retrieves all Brand entities matching the given list of IDs.
     *
     * @param brandIds the list of brand IDs to look up
     * @return a list of Brands whose IDs are contained in {@code brandIds}; empty if none found
     */
    @Override
    public List<Brand> findAllById(List<Long> brandIds) {
        return brandJpaRepository.findAllById(brandIds);
    }
}
