package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * ProductRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 Product 엔티티의 
 * 영속성 작업을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Product> findByIdForUpdate(Long productId) {
        return productJpaRepository.findByIdForUpdate(productId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findAllById(List<Long> productIds) {
        return productJpaRepository.findAllById(productIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findAll(Long brandId, String sort, int page, int size) {
        Pageable pageable = createPageable(sort, page, size);
        Page<Product> productPage = brandId != null
            ? productJpaRepository.findByBrandId(brandId, pageable)
            : productJpaRepository.findAll(pageable);
        return productPage.getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countAll(Long brandId) {
        return brandId != null
            ? productJpaRepository.countByBrandId(brandId)
            : productJpaRepository.count();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLikeCount(Long productId, Long likeCount) {
        Product product = productJpaRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("상품을 찾을 수 없습니다. (상품 ID: %d)", productId)));
        product.updateLikeCount(likeCount);
        productJpaRepository.save(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> findAllProductIds() {
        return productJpaRepository.findAllProductIds();
    }

    private Pageable createPageable(String sort, int page, int size) {
        Sort sortObj = switch (sort != null ? sort : "latest") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount"); // ✅ Product.likeCount 필드로 정렬
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, size, sortObj);
    }
}

