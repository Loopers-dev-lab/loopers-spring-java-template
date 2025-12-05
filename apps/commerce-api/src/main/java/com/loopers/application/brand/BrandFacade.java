package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BrandFacade {
    private final BrandService brandService;

    @Transactional
    public BrandInfo createBrandInfo(String name) {
        Brand brand = brandService.save(Brand.create(name));
        return new BrandInfo(brand.getId(), brand.getName());
    }

    @Transactional
    public List<BrandInfo> createBrandInfoBulk(List<String> names) {
        List<Brand> brands = names.stream()
                .map(Brand::create)
                .toList();
        List<Brand> savedBrands = brandService.saveAll(brands);
        return savedBrands.stream()
                .map(brand -> new BrandInfo(brand.getId(), brand.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BrandInfo> getAllBrands(Pageable pageable) {
        return brandService.getAllBrands(pageable).map(brand -> new BrandInfo(brand.getId(), brand.getName()));
    }
}
