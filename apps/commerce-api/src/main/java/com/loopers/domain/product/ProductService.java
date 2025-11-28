package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Optional<Product> findProductById(Long productId) {
        return productRepository.findProductById(productId);
    }

    public void reduceStock(Product product, Integer quantity) {
        product.reduceStock(quantity);
        productRepository.saveProduct(product);
    }

    public List<Product> findProductsByLatestWithBrandName(Long brandId, int page, int size) {
        return productRepository.findProductsByLatestWithBrandName(brandId, page, size);
    }

    public List<Product> findProductsByPriceAscWithBrandName(Long brandId, int page, int size) {
        return productRepository.findProductsByPriceAscWithBrandName(brandId, page, size);
    }

    public List<Product> findProductsByLikesDescWithBrandName(Long brandId, int page, int size) {
        return productRepository.findProductsByLikesDescWithBrandName(brandId, page, size);
    }

    public Product increaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.increaseLikeCount();
        return product;
    }

    public Product decreaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.decreaseLikeCount();
        return product;
    }
}
