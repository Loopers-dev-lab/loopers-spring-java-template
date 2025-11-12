package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<BrandModel> findAllByStatus(BrandStatus brandStatus) {
        return brandRepository.findAllByStatus(brandStatus);
    }

    @Override
    public BrandModel save(BrandModel brandModel) {
        return brandRepository.save(brandModel);
    }

    @Transactional
    @Override
    public boolean disContinueBrandById(Long id) {
        return brandRepository.findById(id)
                .map(b->{
                    b.setDiscontinued();
                    return true;
                }).orElse(false);
    }

    @Transactional
    @Override
    public boolean disContinueBrandByName(String name) {
        return brandRepository.findByName(name)
                .map(b->{
                    b.setDiscontinued();
                    return true;
                }).orElse(false);
    }
}
