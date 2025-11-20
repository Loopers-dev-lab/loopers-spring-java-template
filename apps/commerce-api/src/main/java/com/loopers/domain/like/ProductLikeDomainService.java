package com.loopers.domain.like;

import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLikeDomainService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductLikeInfo likeProduct(User user, Product product) {
        // 이미 좋아요했는지
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isPresent()) {
            return ProductLikeInfo.from(true, product.getTotalLikes());
        }

        // 좋아요
        ProductLike like = ProductLike.create(user.getId(), product.getId());
        productLikeRepository.save(like);

        // 3. 좋아요 증가 및 저장
        product.increaseLikes();
        productRepository.save(product);

        return ProductLikeInfo.from(true, product.getTotalLikes());
    }

    @Transactional
    public ProductLikeInfo unlikeProduct(User user, Product product) {
        // 좋아요 조회
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isEmpty()) {
            return ProductLikeInfo.from(false, product.getTotalLikes());
        }

        // 좋아요 취소
        productLikeRepository.delete(existingLike.get());

        // 좋아요 감소 및 저장
        product.decreaseLikes();
        productRepository.save(product);

        return ProductLikeInfo.from(false, product.getTotalLikes());
    }

    @Transactional(readOnly = true)
    public List<Product> getLikedProducts(Long userId) {
        // 좋아요한 상품 목록 조회
        List<Long> productIds = productLikeRepository.findProductIdsByUserId(userId);

        if (productIds.isEmpty()) {
            return List.of();
        }

        // 상품 정보 일괄 조회
        return productRepository.findAllByIdIn(productIds);
    }
}
