package com.loopers.application.product;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;


    @Transactional
    public ProductInfo registerProduct(ProductV1Dto.ProductRequest request) {
        Product product = request.toEntity();
        productRepository.save(product);

        return ProductInfo.from(product, 0);
    }

    @Transactional(readOnly = true)
    public List<ProductInfo> findAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> {
                    int likeCount = likeRepository.countByProductId(product.getId());
                    return ProductInfo.from(product, likeCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductInfo findProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "찾고자 하는 상품이 존재하지 않습니다.")
        );

        int likeCount = likeRepository.countByProductId(id);

        return ProductInfo.from(product, likeCount);
    }

    @Transactional(readOnly = true)
    public List<ProductInfo> searchProductsByCondition(ProductV1Dto.SearchProductRequest request) {
        request.sortCondition().conditionValidate();

        List<Product> products = productRepository.searchProductsByCondition(request);

        return products.stream()
                .map(product -> {
                    int likeCount = likeRepository.countByProductId(product.getId());
                    return ProductInfo.from(product, likeCount);
                })
                .toList();
    }
}
