package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    private final LikeRepository likeRepository;

    @Transactional(readOnly = true)
    public Page<ProductModel> getProducts(Pageable pageable, String sort, String brandName) {
        Page<ProductModel> productPage;
        if (brandName != null && !brandName.isBlank()) {
            productPage = productRepository.findByBrandName(brandName, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        List<ProductModel> products = productPage.getContent();
        Map<Long, Long> likeCounts = likeRepository
                .countByProductIdsLiked(products.stream().map(ProductModel::getId).collect(Collectors.toSet()));
        products.forEach(product -> product.setLikeCount(likeCounts.getOrDefault(product.getId(), 0L)));

        // likes_desc 정렬은 메모리에서 처리
        if ("likes_desc".equals(sort)) {
            products.sort((a, b) -> Long.compare(
                    b.getLikeCount() != null ? b.getLikeCount() : 0L,
                    a.getLikeCount() != null ? a.getLikeCount() : 0L));
            
            // 정렬된 리스트로 새로운 Page 객체 생성하여 반환
            return new PageImpl<>(products, pageable, productPage.getTotalElements());
        }

        return productPage;
    }


    @Transactional(readOnly = true)
    public ProductModel getProduct(Long id) {
        ProductModel product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setLikeCount(likeRepository.countByProductLiked(product));
        }
        return product;
    }

    @Transactional(readOnly = true)
    public Optional<Quantity> getQuantity(Long id) {
        ProductModel product = productRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다."));
        return Optional.of(product.getQuantity());
    }

    @Transactional
    public void updateQuantity(Long id, Quantity quantityToDecrease) {
        ProductModel product = productRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다."));

        product.decreaseQuantity(quantityToDecrease);
    }

    @Transactional(readOnly = true)
    public List<ProductModel> getProductsByIds(List<Long> productIds) {
        Set<Long> ids = productIds.stream().collect(Collectors.toSet());
        List<ProductModel> products = productRepository.findAllById(ids);
        Map<Long, Long> likeCounts = likeRepository
                .countByProductIdsLiked(products.stream().map(ProductModel::getId).collect(Collectors.toSet()));
        products.forEach(product -> product.setLikeCount(likeCounts.getOrDefault(product.getId(), 0L)));
        return products;
    }
}
