package com.loopers.domain.product;

import com.loopers.application.product.ProductEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Optional<Product> findProductById(Long productId) {
        return productRepository.findProductById(productId);
    }

    public Product getProductById(Long productId) {
        return productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        product.reduceStock(quantity);

        if (product.isStockZero()) {
            eventPublisher.publishEvent(ProductEvent.StockDepleted.from(productId, 0));
        }
    }

    public void restoreStock(Product product, Integer quantity) {
        product.restoreStock(quantity);
    }

    public List<Product> findProductsByLatest(Long brandId, int page, int size) {
        return productRepository.findProductsByLatest(brandId, page, size);
    }

    public List<Product> findProductsByPriceAsc(Long brandId, int page, int size) {
        return productRepository.findProductsByPriceAsc(brandId, page, size);
    }

    public List<Product> findProductsByLikesDesc(Long brandId, int page, int size) {
        return productRepository.findProductsByLikesDesc(brandId, page, size);
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByIds(List<Long> productIds) {
        return productRepository.findProductsByIds(productIds);
    }

    @Transactional
    public Product increaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.increaseLikeCount();
        productRepository.saveProduct(product);
        return product;
    }

    @Transactional
    public Product decreaseLikeCount(Long productId) {
        Product product = productRepository.findProductById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        product.decreaseLikeCount();
        productRepository.saveProduct(product);
        return product;
    }
}
