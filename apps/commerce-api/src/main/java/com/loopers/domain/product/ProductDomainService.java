package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
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
    public List<Product> getProducts(
            Long brandId,
            ProductSortType sortType,
            int page,
            int size
    ) {
        if (brandId != null) {
            return productRepository.findByBrandId(brandId, sortType, page, size);
        } else {
            return productRepository.findAll(sortType, page, size);
        }
    }

    /**
     * 상품 단건 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 상품을 찾을 수 없습니다."
                ));
    }

    /**
     * 여러 상품 조회
     */
    public List<Product> findByIds(List<Long> productIds) {
        return productIds.stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new CoreException(
                                ErrorType.NOT_FOUND,
                                "해당 상품을 찾을 수 없습니다."
                        )))
                .toList();
    }

    /**
     * 재고 차감
     */
    @Transactional
    public void decreaseStock(Long productId, Long quantity) {
        Product product = getProduct(productId);

        if (!product.hasEnoughStock(quantity)) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    String.format("상품 '%s'의 재고가 부족합니다.", product.getName())
            );
        }

        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    public List<Product> getProductsByBrandId(Long brandId) {

        return productRepository.findByBrandId(brandId);
    }
}
