package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductWithBrand;
import com.loopers.interfaces.api.product.ProductDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductDomainService productDomainService;

    public ProductDto.ProductDetailResponse getProduct(Long productId) {

        ProductWithBrand productWithBrand = productDomainService.getProductWithBrand(productId);

        return ProductDto.ProductDetailResponse.from(productWithBrand);
    }

    public ProductDto.ProductListResponse getProducts(
            Long brandId,
            String sort,
            int page,
            int size
    ) {
        // 브랜드 검증
        if (brandId != null) {
            validateBrand(brandId);
        }

        ProductSortType sortType = ProductSortType.from(sort);

        List<Product> products;
        if (brandId != null) {
            products = productRepository.findByBrandId(brandId, sortType, page, size);
        } else {
            products = productRepository.findAll(sortType, page, size);
        }

        List<ProductWithBrand> productsWithBrand = productDomainService.getProductsWithBrand(products);

        return ProductDto.ProductListResponse.from(productsWithBrand);
    }

    private void validateBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 브랜드를 찾을 수 없습니다."
                ));

        if (!brand.isActive()) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "해당 브랜드를 찾을 수 없습니다."
            );
        }
    }
}
