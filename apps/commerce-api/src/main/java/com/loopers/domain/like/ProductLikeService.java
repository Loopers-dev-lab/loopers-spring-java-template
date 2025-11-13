package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.like.ProductLikeDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class ProductLikeService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;

    public ProductLikeDto.LikeResponse likeProduct(String userId, Long productId) {
        // 1. 사용자 조회
        User user = userRepository.find(userId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 사용자를 찾을 수 없습니다."
                ));

        // 2. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 상품을 찾을 수 없습니다."
                ));

        // 3. 이미 좋아요했는지
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), productId);

        if (existingLike.isPresent()) {
            return ProductLikeDto.LikeResponse.from(true, product.getTotalLikes());
        }

        // 4. 좋아요 생성
        ProductLike like = ProductLike.create(user.getId(), productId);
        productLikeRepository.save(like);

        // 5. 상품의 totalLikes 증가
        product.increaseLikes();
        Product updatedProduct = productRepository.save(product);

        return ProductLikeDto.LikeResponse.from(true, updatedProduct.getTotalLikes());
    }

    @Transactional
    public ProductLikeDto.LikeResponse unlikeProduct(String userId, Long productId) {
        // 1. 사용자 조회
        User user = userRepository.find(userId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 사용자를 찾을 수 없습니다."
                ));

        // 2. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 상품을 찾을 수 없습니다."
                ));

        // 3. 좋아요 조회
        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), productId);

        if (existingLike.isEmpty()) {
            return ProductLikeDto.LikeResponse.from(false, product.getTotalLikes());
        }

        // 4. 좋아요 삭제
        productLikeRepository.delete(existingLike.get());

        // 5. 상품의 totalLikes 감소
        product.decreaseLikes();
        Product updatedProduct = productRepository.save(product);

        return ProductLikeDto.LikeResponse.from(false, updatedProduct.getTotalLikes());
    }
}
