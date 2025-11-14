package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.like.LikeV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public LikeInfo doLike(LikeV1Dto.LikeRequest request) {
        /*
        - [ ] 사용자 검증
        - [ ] 상품 검증
        - [ ] 좋아요 등록 (멱등)
         */
        Long userId = request.userId();
        Long productId = request.productId();

        userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저입니다.")
        );

        productRepository.findById(productId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        );

        return likeRepository.findByUserIdAndProductId(userId, productId)
                .map(LikeInfo::from)
                .orElseGet(() -> {
                    Like newLike = request.toEntity();
                    likeRepository.save(newLike);

                    return LikeInfo.from(newLike);
                });
    }

    @Transactional
    public void doUnlike(Long userId, Long productId) {
        /*
        - [ ] 사용자 검증
        - [ ] 상품 검증
        - [ ] 좋아요 취소 (멱등)
         */

        userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저입니다.")
        );

        productRepository.findById(productId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        );

        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(likeRepository::delete);

    }
}
