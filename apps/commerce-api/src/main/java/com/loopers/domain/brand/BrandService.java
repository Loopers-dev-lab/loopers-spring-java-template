package com.loopers.domain.brand;

import com.loopers.application.brand.BrandCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandService {
    private final BrandRepository brandRepository;


    @Transactional(readOnly = true)
    public BrandModel getBrandById(Long id) {
        return brandRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public BrandModel getBrandByName(String name) {
        return brandRepository.findByName(name).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<BrandModel> getBrands() {
        return null;
    }

    @Transactional
    public BrandModel createModel(BrandCommand.Create command) {

        if(brandRepository.existsByName(command.name())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용중인 브랜드명 입니다.");
        }
        BrandModel brand = new BrandModel(command.name(), command.description(), command.status());
        return brandRepository.save(brand);
    }





}
