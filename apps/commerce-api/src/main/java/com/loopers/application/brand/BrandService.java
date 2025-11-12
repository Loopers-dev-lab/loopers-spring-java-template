package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public Brand createBrand(String name, String description) {
        if (brandRepository.existsByName(name)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 브랜드입니다.");
        }

        Brand brand = Brand.builder()
            .name(name)
            .description(description)
            .build();

        return brandRepository.save(brand);
    }

    public Brand getBrand(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    public Brand getBrandByName(String name) {
        return brandRepository.findByName(name)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    @Transactional
    public Brand updateBrand(Long id, String name, String description) {
        Brand brand = getBrand(id);

        if (name != null && !name.equals(brand.getName())) {
            if (brandRepository.existsByName(name)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 브랜드명입니다.");
            }
            brand.updateName(name);
        }

        if (description != null) {
            brand.updateDescription(description);
        }

        return brand;
    }
}
