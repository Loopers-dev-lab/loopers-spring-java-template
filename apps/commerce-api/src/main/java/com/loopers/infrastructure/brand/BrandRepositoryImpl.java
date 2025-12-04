package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long brandId) {
        return brandJpaRepository.findById(brandId);
    }

    @Override
    public String findNameById(Long brandId) {
        return brandJpaRepository.findNameById(brandId);
    }

    @Override
    public Map<Long, String> findBrandNamesByIds(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Map.of();
        }
        
        return brandJpaRepository.findByIds(brandIds).stream()
                .collect(Collectors.toMap(
                        Brand::getId,
                        Brand::getName
                ));
    }
}

