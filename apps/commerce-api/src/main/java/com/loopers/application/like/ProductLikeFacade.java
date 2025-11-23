package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserDomainService;
import com.loopers.interfaces.api.like.ProductLikeDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeFacade {

    private final ProductLikeDomainService productLikeDomainService;
    private final ProductDomainService productDomainService;
    private final UserDomainService userDomainService;


    public ProductLikeDto.LikeResponse likeProduct(String userId, Long productId) {
        // 사용자 조회
        User user = userDomainService.findUser(userId);

        // 좋아요 - 낙관적 락 예외 발생 가능
        try {
            ProductLikeInfo info = productLikeDomainService.likeProduct(user, productId);
            return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new CoreException(
                    ErrorType.CONFLICT,
                    "일시적인 오류가 발생했습니다. 다시 시도해주세요."
            );
        }
    }

    public ProductLikeDto.LikeResponse unlikeProduct(String userId, Long productId) {
        // 사용자 조회
        User user = userDomainService.findUser(userId);

        // 좋아요 취소 - 낙관적 락 예외 발생 가능
        try {
            ProductLikeInfo info = productLikeDomainService.unlikeProduct(user, productId);
            return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());

        } catch (
                ObjectOptimisticLockingFailureException e) {
            throw new CoreException(
                    ErrorType.CONFLICT,
                    "일시적인 오류가 발생했습니다. 다시 시도해주세요."
            );
        }

    }

    public ProductLikeDto.LikedProductsResponse getLikedProducts(String userId) {
        // 사용자
        User user = userDomainService.findUser(userId);

        // 좋아요한 목록 조회
        List<Product> products = productLikeDomainService.getLikedProducts(user.getId());

        return ProductLikeDto.LikedProductsResponse.from(products);
    }
}
