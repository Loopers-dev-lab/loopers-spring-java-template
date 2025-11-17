package com.loopers.domain.brand;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository {

    Optional<Brand> findById(Long brandId);

    Map<Long, String> findBrandNamesByIds(List<Long> brandIds);

}

