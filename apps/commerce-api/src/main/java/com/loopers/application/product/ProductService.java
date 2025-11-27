package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public Product createProduct(Long brandId, String name, BigDecimal price, Integer stock,
        String description) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));

        Product product = Product.builder()
            .brand(brand)
            .name(name)
            .price(price)
            .stock(stock)
            .description(description)
            .build();

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        // 트랜잭션 내에서 Brand를 로딩하여 DTO로 변환
        return products.map(product -> {
            // Brand를 명시적으로 로딩
            product.getBrand().getName();
            return ProductInfo.from(product);
        });
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProductsByBrand(Long brandId, Pageable pageable) {
        Page<Product> products = productRepository.findByBrandId(brandId, pageable);
        // 트랜잭션 내에서 Brand를 로딩하여 DTO로 변환
        return products.map(product -> {
            // Brand를 명시적으로 로딩
            product.getBrand().getName();
            return ProductInfo.from(product);
        });
    }

    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public Product updateProduct(Long id, String name, BigDecimal price, Integer stock,
        String description) {
        Product product = getProduct(id);
        product.updateInfo(name, price, stock, description);
        return product;
    }
}
