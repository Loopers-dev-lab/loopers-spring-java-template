package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductDomainService {

    private final ProductRepository productRepository;

    /**
     * 상품 목록 조회
     */
    public Page<Product> getProducts(
            Long brandId,
            ProductSortType sortType,
            int page,
            int size
    ) {
        Page<Product> productPage;

        if (brandId != null) {
            productPage = productRepository.findByBrandId(brandId, sortType, page, size);
        } else {
            productPage = productRepository.findAll(sortType, page, size);
        }

        return productPage;
    }

    /**
     * 상품 단건 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findByIdOrThrow(productId);
    }

    /**
     * 재고 차감
     */
    public Product decreaseStock(Long productId, Long quantity) {
        Product product = getProductWithLock(productId);

        if (!product.hasEnoughStock(quantity)) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    String.format("상품 '%s'의 재고가 부족합니다.", product.getName())
            );
        }

        product.decreaseStock(quantity);
        productRepository.save(product);

        return product;
    }

    /**
     * 비관적 락 상품조회
     */
    private Product getProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 상품을 찾을 수 없습니다."
                ));
    }

    public List<Product> getProductsByBrandId(Long brandId) {

        return productRepository.findByBrandId(brandId);
    }
}
