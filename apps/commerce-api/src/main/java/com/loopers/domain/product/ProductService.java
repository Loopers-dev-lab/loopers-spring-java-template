package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final EntityManager entityManager;

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductInfo getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        Brand brand = entityManager.find(Brand.class, product.getBrandId());
        String brandName = (brand != null) ? brand.getName() : null;
        long likeCount = likeRepository.countByProductId(product.getId());

        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getStockQuantity(),
                product.getPrice().getAmount(),
                brandName,
                likeCount
        );
    }
}
