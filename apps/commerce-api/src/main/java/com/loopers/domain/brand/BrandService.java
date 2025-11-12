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

        BrandModel brand = brandRepository.findByName(name).orElse(null);
        if(brand == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "등록되지 않은 브랜드명입니다.");
        }
        return brand;
    }

    @Transactional(readOnly = true)
    public List<BrandModel> getBrands() {
        return brandRepository.findAllByStatus(BrandStatus.REGISTERED);
    }

    @Transactional
    public BrandModel createBrandModel(BrandCommand.Create command) {
        BrandModel brandModel;

        if(brandRepository.existsByName(command.name())) {
            brandModel = brandRepository.findByName(command.name()).orElse(null);
            if(brandModel == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 정보 상세 조회에 실패하였습니다.");
            }

            if(brandModel.isRegistered()) {
                throw new CoreException(ErrorType.CONFLICT, "이미 사용중인 브랜드명 입니다.");
            }
            brandModel.setRegistered();
        } else {
            brandModel = new BrandModel(command.name(), command.description(), command.status());
        }
        return brandRepository.save(brandModel);
    }

}
