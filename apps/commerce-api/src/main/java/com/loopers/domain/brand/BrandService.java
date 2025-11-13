package com.loopers.domain.brand;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.brand.BrandDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandDto.BrandDetailResponse getBrand(Long brandId) {
        // 1. 브랜드 조회
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND, "해당 브랜드를 찾을 수 없습니다."
                ));

        // 2. 활성 상태 확인
        if (!brand.isActive()) {
            throw new CoreException(
                    ErrorType.NOT_FOUND, "해당 브랜드를 찾을 수 없습니다."
            );
        }

        // 3. 해당 브랜드의 상품 목록 조회
        List<Product> products = productRepository.findByBrandId(brandId);

        return BrandDto.BrandDetailResponse.from(brand, products);
    }
}
