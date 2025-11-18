package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.interfaces.api.product.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductDomainService productDomainService;
    private final BrandDomainService brandDomainService;

    /**
     * 상품 목록 조회
     */
    public ProductDto.ProductListResponse getProducts(
            Long brandId,
            String sort,
            int page,
            int size
    ) {
        // 브랜드 검증
        if (brandId != null) {
            brandDomainService.getActiveBrand(brandId);
        }

        // 상품 조회
        ProductSortType sortType = ProductSortType.from(sort);
        Page<Product> products = productDomainService.getProducts(
                brandId, sortType, page, size
        );

        // 브랜드 정보 조회
        Set<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandDomainService.getBrandMap(brandIds);


        return ProductDto.ProductListResponse.from(products, brandMap);
    }

    /**
     * 상품 상세 조회
     */
    public ProductDto.ProductDetailResponse getProduct(Long productId) {
        // 1. 상품 조회
        Product product = productDomainService.getProduct(productId);

        // 2. 브랜드 조회
        Brand brand = brandDomainService.getBrand(product.getBrandId());

        // 3. DTO 변환
        return ProductDto.ProductDetailResponse.from(product, brand);
    }
}
