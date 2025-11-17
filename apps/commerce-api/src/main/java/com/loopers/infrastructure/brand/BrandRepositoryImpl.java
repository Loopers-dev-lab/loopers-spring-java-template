package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class BrandRepositoryImpl implements BrandRepository {

    @Override
    public Optional<Brand> findById(Long brandId) {
        return Optional.empty();
    }

    @Override
    public Map<Long, String> findBrandNamesByIds(List<Long> brandIds) {
        return new HashMap<>();
    }
}

