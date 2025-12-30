package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductService
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCache productCache;


    @Transactional(readOnly = true)
    public Page<Product> getProducts(Long brandId, Pageable sortedPageable) {
        return productCache.getProductList(brandId, sortedPageable)
                .orElseGet(() -> {
                    Page<Product> products = fetchProducts(brandId, sortedPageable);
                    productCache.putProductList(brandId, sortedPageable, products);
                    return products;
                });
    }

    public Product getProduct(Long productId) {
        return productCache.getProductDetail(productId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품이 없습니다"));
                    productCache.putProductDetail(productId, product);
                    return product;
                });
    }

    private Page<Product> fetchProducts(Long brandId, Pageable sortedPageable) {
        return (brandId == null)
                ? productRepository.findAll(sortedPageable)
                : productRepository.findByBrandId(brandId, sortedPageable);
    }
}
