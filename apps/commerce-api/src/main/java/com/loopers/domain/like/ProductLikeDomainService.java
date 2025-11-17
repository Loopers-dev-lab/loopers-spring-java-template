package com.loopers.domain.like;

import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLikeDomainService {

    private final ProductLikeRepository productLikeRepository;

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

        // 3. 상품의 totalLikes 증가
        product.increaseLikes();

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

        // 3. 상품의 totalLikes 감소
        product.decreaseLikes();

        return ProductLikeInfo.from(false, product.getTotalLikes());
    }
}
