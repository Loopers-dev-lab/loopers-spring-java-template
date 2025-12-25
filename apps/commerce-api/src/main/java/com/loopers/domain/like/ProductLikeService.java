package com.loopers.domain.like;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.activity.event.UserActivityEvent;
import com.loopers.domain.like.event.ProductLikeAddedEvent;
import com.loopers.domain.like.event.ProductLikeRemovedEvent;
import com.loopers.domain.outbox.OutboxEventService;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.kafka.AggregateTypes;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.loopers.kafka.KafkaTopics.ProductLike.*;
import static com.loopers.kafka.KafkaTopics.UserActivity;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProductLike addLike(User user, Product product) {
        // 멱등성 처리: 이미 좋아요가 존재하면 기존 것을 반환
        return productLikeRepository.findByLikeUserAndLikeProduct(user, product)
                .orElseGet(() -> {
                    // 좋아요가 없는 경우에만 새로 생성
                    ProductLike like = ProductLike.addLike(user, product);
                    ProductLike savedLike = productLikeRepository.save(like);

                    // 좋아요 집계 처리 이벤트 발행
                    publishProductLikeAddedEvent(product, savedLike);

                    // 사용자 행동 추적 이벤트 발행
                    publishUserActivityEvent(user, product, "PRODUCT_LIKE_ADDED");

                    return savedLike;
                });
    }

    /**
     * 상품 좋아요 집계 이벤트 발행
     * 실패 시에도 좋아요 추가 트랜잭션에 영향을 주지 않음
     * */
    private void publishProductLikeAddedEvent(Product product, ProductLike savedLike) {

        try {
            // Product의 좋아요 수(집계) 증가 이벤트 분리
            ProductLikeAddedEvent productLikeAddedEvent = ProductLikeAddedEvent.of(
                    savedLike.getId(),
                    product.getId()
            );

            String likeAddedPayload = objectMapper.writeValueAsString(productLikeAddedEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.PRODUCT_LIKE,
                    product.getId().toString(),
                    LIKE_ADDED,
                    likeAddedPayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 저장
            log.error("ProductLikeAddedEvent 직렬화 실패 - 좋아요 추가는 성공 처리됨. productLikeId: {}, productId: {}",
                    product.getId(), savedLike.getId(), e);
        }
    }

    /**
     * 사용자 활동 이벤트 발행
     * 실패 시에도 좋아요 트랜잭션에 영향을 주지 않음
     */
    private void publishUserActivityEvent(User user, Product product, String activityType) {
        try {
            UserActivityEvent userActivityEvent = UserActivityEvent.of(
                    user.getUserId(),
                    activityType,
                    "PRODUCT",
                    product.getId()
            );

            String userActivityPayload = objectMapper.writeValueAsString(userActivityEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.ACTIVITY,
                    product.getId().toString(),
                    UserActivity.USER_ACTIVITY,
                    userActivityPayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 남기고 좋아요 처리는 성공
            log.error("UserActivityEvent 직렬화 실패 - 좋아요 처리는 성공 처리됨. productId: {}, userId: {}, activityType: {}",
                    product.getId(), user.getId(), activityType, e);
        }
    }

    @Transactional
    public void cancelLike(User user, Product product) {
        ProductLike like = productLikeRepository.findByLikeUserAndLikeProduct(user, product)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "좋아요가 존재하지 않습니다"));

        // ProductLike hard delete 처리
        productLikeRepository.delete(like);

        // 좋아요 집계 처리 이벤트 발행
        publishProductLikeRemovedEvent(product, like);

        // 사용자 행동 추적 이벤트 발행
        publishUserActivityEvent(user, product, "PRODUCT_LIKE_CANCELLED");
    }

    /**
     * 상품 좋아요 취소 집계 이벤트 발행
     * 실패 시에도 좋아요 취소 트랜잭션에 영향을 주지 않음
     */
    private void publishProductLikeRemovedEvent(Product product, ProductLike like) {
        try {
            ProductLikeRemovedEvent productLikeRemovedEvent = ProductLikeRemovedEvent.of(
                    like.getId(),
                    product.getId()
            );

            String likeRemovedPayload = objectMapper.writeValueAsString(productLikeRemovedEvent);

            outboxEventService.createOutboxEvent(
                    AggregateTypes.PRODUCT_LIKE,
                    product.getId().toString(),
                    LIKE_REMOVED,
                    likeRemovedPayload
            );
        } catch (JsonProcessingException e) {
            // 이벤트 발행 실패 시 로그만 저장
            log.error("ProductLikeRemovedEvent 직렬화 실패 - 좋아요 취소는 성공 처리됨. productLikeId: {}, productId: {}",
                    like.getId(), product.getId(), e);
        }
    }
}
