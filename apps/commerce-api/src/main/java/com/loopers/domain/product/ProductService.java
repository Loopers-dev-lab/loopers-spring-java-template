package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.interfaces.api.product.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductDto.ProductListResponse getProducts(
            Long brandId,
            String sort,
            Integer page,
            Integer size
    ) {

        List<Product> products;

        if (brandId != null) {
            products = productRepository.findByBrandId(brandId, page, size);
        } else {
            products = productRepository.findAll(page, size);
        }

        // 브랜드 정보 조회
        Map<Long, Brand> brandMap = getBrandMap(products);

        return ProductDto.ProductListResponse.from(products, brandMap);
    }

    private Map<Long, Brand> getBrandMap(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        Set<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());

        List<Brand> brands = brandRepository.findByIdIn(brandIds);

        return brands.stream()
                .collect(Collectors.toMap(Brand::getId, brand -> brand));
    }
}
