package com.loopers.application.like;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.userbehavior.UserBehaviorEvent;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ProductService productService;
    private final BrandService brandService;
    private final ApplicationEventPublisher eventPublisher;

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 10),
            recover = "recoverRecordLike"
    )
    @Transactional
    public LikeInfo recordLike(LikeCommand.LikeProductCommand command) {
        User user = userService.findUserByLoginId(command.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        boolean wasCreated = likeService.recordLikeIfAbsent(user.getId(), command.productId());

        if (wasCreated) {
            eventPublisher.publishEvent(LikeEvent.LikeRecorded.from(command.productId(), user.getId()));
            eventPublisher.publishEvent(UserBehaviorEvent.LikeRecorded.from(user.getId(), command.productId()));
        }

        Product product = productService.findProductById(command.productId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        return LikeInfo.from(
                product.getId(),
                product.getLikeCount().getCount()
        );
    }

    @Recover
    public LikeInfo recoverRecordLike(RuntimeException e, LikeCommand.LikeProductCommand command) {
        if (e instanceof OptimisticLockingFailureException) {
            throw new CoreException(ErrorType.CONFLICT, "좋아요 등록 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
        }
        throw e;
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 10),
            recover = "recoverCancelLike"
    )
    @Transactional
    public LikeInfo cancelLike(LikeCommand.LikeProductCommand command) {
        User user = userService.findUserByLoginId(command.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        boolean wasDeleted = likeService.cancelLikeIfPresent(user.getId(), command.productId());

        if (wasDeleted) {
            eventPublisher.publishEvent(LikeEvent.LikeCancelled.from(command.productId(), user.getId()));
            eventPublisher.publishEvent(UserBehaviorEvent.LikeCancelled.from(user.getId(), command.productId()));
        }

        Product product = productService.findProductById(command.productId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        return LikeInfo.from(
                product.getId(),
                product.getLikeCount().getCount()
        );
    }

    @Recover
    public LikeInfo recoverCancelLike(RuntimeException e, LikeCommand.LikeProductCommand command) {
        if (e instanceof OptimisticLockingFailureException) {
            throw new CoreException(ErrorType.CONFLICT, "좋아요 취소 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
        }
        throw e;
    }

    @Transactional(readOnly = true)
    public List<ProductInfo> getLikedProducts(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<Long> productIds = likeService.findLikedProductIds(user.getId());

        if (productIds.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "좋아요한 상품이 없습니다.");
        }

        List<Product> products = productIds.stream()
                .map(productService::findProductById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet())
                .stream()
                .toList();

        Map<Long, String> brandNamesMap = brandService.findBrandNamesByIds(brandIds);

        return products.stream()
                .map(product -> ProductInfo.from(product, brandNamesMap.get(product.getBrandId())))
                .toList();
    }

}
