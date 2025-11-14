package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductDomainService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductWithBrand getProductWithBrand(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 상품을 찾을 수 없습니다."
                ));

        // 2. 브랜드 조회
        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 브랜드를 찾을 수 없습니다."
                ));

        // 3. 도메인 객체 조합
        return new ProductWithBrand(product, brand);
    }

    public List<ProductWithBrand> getProductsWithBrand(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        Map<Long, Brand> brandMap = getBrandMap(products);

        // Product와 Brand 조합
        return products.stream()
                .map(product -> {
                    Brand brand = brandMap.get(product.getBrandId());
                    if (brand == null) {
                        throw new CoreException(
                                ErrorType.NOT_FOUND,
                                "브랜드를 찾을 수 없습니다: " + product.getBrandId()
                        );
                    }
                    return new ProductWithBrand(product, brand);
                })
                .toList();
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
