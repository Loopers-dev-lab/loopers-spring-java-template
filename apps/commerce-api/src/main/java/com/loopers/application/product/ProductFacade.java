package com.loopers.application.product;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final BrandRepository brandRepository;


    @Transactional
    public ProductInfo registerProduct(ProductV1Dto.ProductRequest request) {
        Long brandId = request.brandId();

        brandRepository.findById(brandId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "등록하고자 하는 상품의 브랜드가 존재하지 않습니다.")
        );

        Product product = request.toEntity();
        productRepository.save(product);

        return ProductInfo.from(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductInfo> findAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(ProductInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductInfo findProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "찾고자 하는 상품이 존재하지 않습니다.")
        );

        return ProductInfo.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductInfo> searchProductsByCondition(ProductV1Dto.SearchProductRequest request) {
        request.filterCondition().conditionValidate();
        request.sortCondition().conditionValidate();

        List<Product> products = productRepository.searchProductsByCondition(request);

        return products.stream()
                .map(ProductInfo::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "product", key = "#request.id()")
    public ProductInfo changePrice(ProductV1Dto.ChangePriceRequest request) {
        Long id = request.id();
        BigDecimal newPrice = request.newPrice();

        Product product = productRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        );

        Product changedProduct = product.changePrice(newPrice);

        return ProductInfo.from(changedProduct);
    }
}
