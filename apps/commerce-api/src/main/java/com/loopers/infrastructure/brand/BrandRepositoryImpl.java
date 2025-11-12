package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandRepository;

    @Override
    public boolean existsById(Long id) {
        return brandRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String brandName) {
        return brandRepository.existsByName(brandName);
    }

    @Override
    public Optional<BrandModel> findById(Long id) {
        return brandRepository.findById(id);
    }

    @Override
    public Optional<BrandModel> findByName(String brandName) {
        return brandRepository.findByName(brandName);
    }

    @Override
    public boolean deleteBrand(Long id) {
        BrandModel brandModel = brandRepository.findById(id).orElse(null);

        if(brandModel == null) {
            return false;
        }
        brandModel.setStatus('0');
        brandRepository.save(brandModel);

        return true;
    }

    @Override
    public BrandModel save(BrandModel brandModel) {
        return brandRepository.save(brandModel);
    }
}
