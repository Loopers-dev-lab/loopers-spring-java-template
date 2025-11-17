package com.loopers.domain.like;

import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLikeDomainService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;

    public ProductLikeInfo likeProduct(User user, Product product) {
        // 1. 이미 좋아요했는지
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isPresent()) {
            return ProductLikeInfo.from(true, product.getTotalLikes());
        }

        // 2. 좋아요 생성
        ProductLike like = ProductLike.create(user.getId(), product.getId());
        productLikeRepository.save(like);

        // 3. 좋아요 증가 및 저장
        product.increaseLikes();
        productRepository.save(product);

        return ProductLikeInfo.from(true, product.getTotalLikes());
    }

    public ProductLikeInfo unlikeProduct(User user, Product product) {
        // 1. 좋아요 조회
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existingLike.isEmpty()) {
            return ProductLikeInfo.from(false, product.getTotalLikes());
        }

        // 2. 좋아요 삭제
        productLikeRepository.delete(existingLike.get());

        // 3. 좋아요 감소 및 저장
        product.decreaseLikes();
        productRepository.save(product);

        return ProductLikeInfo.from(false, product.getTotalLikes());
    }

    public List<Product> getLikedProducts(Long userId) {
        // 1. 좋아요한 ProductId 목록 조회
        List<Long> productIds = productLikeRepository.findProductIdsByUserId(userId);

        // 2. 상품 정보 일괄 조회
        if (productIds.isEmpty()) {
            return List.of();
        }

        return productRepository.findAllByIdIn(productIds);
    }
}
