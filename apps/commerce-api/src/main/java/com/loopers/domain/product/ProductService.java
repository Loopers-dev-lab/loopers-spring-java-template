package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class ProductService {
    private final ProductRepository productRepository;

    public Product getProduct(final Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + productId + "] 상품을 찾을 수 없습니다."));
    }

    public List<Product> getProductList(ProductSortType sortType) {
        return productRepository.getProductList(sortType);
    }

    @Transactional
    public void likeCountIncrease(final Long productId, final int amount) {
        getProduct(productId).likeCountIncrease(amount);
    }

    @Transactional
    public void likeCountDecrease(final Long productId, final int amount) {
        getProduct(productId).likeCountDecrease(amount);
    }

    public List<Product> getProductIn(final List<Long> productIdList) {
        return productRepository.findByIdIn(productIdList);
    }
}
