package com.loopers.core.service.productlike.component;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.service.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("ProductLikeSynchronizer 통합테스트")
class ProductLikeSynchronizerTest extends IntegrationTest {

    @Autowired
    private ProductLikeSynchronizer productLikeSynchronizer;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Nested
    @DisplayName("좋아요 캐시 동기화")
    class 좋아요_캐시_동기화 {

        private final ProductId productId = new ProductId("1");
        private final UserId userId = new UserId("1");

        @Nested
        @DisplayName("좋아요 목록이 비어있고 취소 목록도 비어있는 경우")
        class 좋아요_취소_목록이_비어있는_경우 {

            @Test
            @DisplayName("동기화 시 아무 것도 저장되지 않는다")
            void 아무것도_저장되지_않는다() {
                List<ProductLikeCache> emptyLikes = List.of();
                List<ProductLikeCache> emptyUnlikes = List.of();

                productLikeSynchronizer.sync(emptyLikes, emptyUnlikes);

                List<ProductLike> allLikes = productLikeRepository.findAll();
                assertThat(allLikes)
                        .as("좋아요가 저장되지 않아야 함")
                        .isEmpty();
            }
        }

        @Nested
        @DisplayName("캐시된 좋아요를 동기화하는 경우")
        class 캐시된_좋아요를_동기화하는_경우 {

            @Test
            @DisplayName("좋아요가 데이터베이스에 저장된다")
            void 좋아요가_데이터베이스에_저장된다() {
                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedLike = new ProductLikeCache(
                        productId,
                        userId,
                        currentTime
                );

                productLikeSynchronizer.sync(List.of(cachedLike), List.of());

                List<ProductLike> savedLikes = productLikeRepository.findAll();
                assertSoftly(softly -> {
                    softly.assertThat(savedLikes)
                            .as("좋아요가 저장되어야 함")
                            .hasSize(1);
                    softly.assertThat(savedLikes.get(0).getProductId())
                            .as("상품 ID가 일치해야 함")
                            .isEqualTo(productId);
                    softly.assertThat(savedLikes.get(0).getUserId())
                            .as("사용자 ID가 일치해야 함")
                            .isEqualTo(userId);
                });
            }

            @Test
            @DisplayName("여러 사용자의 좋아요가 저장된다")
            void 여러_사용자의_좋아요가_저장된다() {
                UserId anotherUserId = new UserId("2");

                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedLike1 = new ProductLikeCache(
                        productId,
                        userId,
                        currentTime
                );
                ProductLikeCache cachedLike2 = new ProductLikeCache(
                        productId,
                        anotherUserId,
                        currentTime + 100
                );

                productLikeSynchronizer.sync(List.of(cachedLike1, cachedLike2), List.of());

                List<ProductLike> savedLikes = productLikeRepository.findAll();
                assertThat(savedLikes)
                        .as("모든 사용자의 좋아요가 저장되어야 함")
                        .hasSize(2)
                        .anyMatch(like -> like.getUserId().equals(userId))
                        .anyMatch(like -> like.getUserId().equals(anotherUserId));
            }

            @Test
            @DisplayName("캐시 타임스탬프로 CreatedAt이 설정된다")
            void 캐시_타임스탐프로_CreatedAt이_설정된다() {
                long timestamp = System.currentTimeMillis();
                ProductLikeCache cachedLike = new ProductLikeCache(
                        productId,
                        userId,
                        timestamp
                );

                productLikeSynchronizer.sync(List.of(cachedLike), List.of());

                List<ProductLike> savedLikes = productLikeRepository.findAll();
                ProductLike savedLike = savedLikes.get(0);

                LocalDateTime expectedDateTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp),
                        ZoneId.systemDefault()
                );

                assertThat(savedLike.getCreatedAt().value())
                        .as("CreatedAt이 타임스탐프로부터 변환되어야 함")
                        .isEqualTo(expectedDateTime);
            }
        }

        @Nested
        @DisplayName("캐시된 취소를 동기화하는 경우")
        class 캐시된_취소를_동기화하는_경우 {

            @Test
            @DisplayName("캐시된 취소가 데이터베이스에서 삭제된다")
            void 캐시된_취소가_삭제된다() {
                // 먼저 좋아요를 저장
                ProductLike productLike = ProductLike.create(
                        userId,
                        productId,
                        new CreatedAt(LocalDateTime.now())
                );
                productLikeRepository.save(productLike);

                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedUnlike = new ProductLikeCache(
                        productId,
                        userId,
                        currentTime
                );

                productLikeSynchronizer.sync(List.of(), List.of(cachedUnlike));

                List<ProductLike> remainingLikes = productLikeRepository.findAll();
                assertThat(remainingLikes)
                        .as("좋아요가 삭제되어야 함")
                        .isEmpty();
            }

            @Test
            @DisplayName("여러 사용자의 취소가 처리된다")
            void 여러_사용자의_취소가_처리된다() {
                UserId anotherUserId = new UserId("2");

                // 추가 사용자의 좋아요 저장
                ProductLike like1 = ProductLike.create(
                        userId,
                        productId,
                        new CreatedAt(LocalDateTime.now())
                );
                ProductLike like2 = ProductLike.create(
                        anotherUserId,
                        productId,
                        new CreatedAt(LocalDateTime.now())
                );
                productLikeRepository.save(like1);
                productLikeRepository.save(like2);

                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedUnlike1 = new ProductLikeCache(
                        productId,
                        userId,
                        currentTime
                );
                ProductLikeCache cachedUnlike2 = new ProductLikeCache(
                        productId,
                        anotherUserId,
                        currentTime + 100
                );

                productLikeSynchronizer.sync(List.of(), List.of(cachedUnlike1, cachedUnlike2));

                List<ProductLike> remainingLikes = productLikeRepository.findAll();
                assertThat(remainingLikes)
                        .as("모든 좋아요가 삭제되어야 함")
                        .isEmpty();
            }
        }

        @Nested
        @DisplayName("좋아요와 취소가 함께 동기화되는 경우")
        class 좋아요와_취소가_함께_동기화되는_경우 {

            @Test
            @DisplayName("좋아요는 추가되고 취소는 삭제된다")
            void 좋아요는_추가되고_취소는_삭제된다() {
                UserId newUserId = new UserId("2");

                // 먼저 기존 좋아요 저장
                ProductLike existingLike = ProductLike.create(
                        userId,
                        productId,
                        new CreatedAt(LocalDateTime.now())
                );
                productLikeRepository.save(existingLike);

                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedLike = new ProductLikeCache(
                        productId,
                        newUserId,
                        currentTime
                );
                ProductLikeCache cachedUnlike = new ProductLikeCache(
                        productId,
                        userId,
                        currentTime + 100
                );

                productLikeSynchronizer.sync(List.of(cachedLike), List.of(cachedUnlike));

                List<ProductLike> remainingLikes = productLikeRepository.findAll();
                assertSoftly(softly -> {
                    softly.assertThat(remainingLikes)
                            .as("새로운 사용자의 좋아요만 남아야 함")
                            .hasSize(1);
                    softly.assertThat(remainingLikes.get(0).getUserId())
                            .as("새로운 사용자 ID여야 함")
                            .isEqualTo(newUserId);
                });
            }
        }

        @Nested
        @DisplayName("같은 상품에 대한 여러 좋아요가 동기화되는 경우")
        class 같은_상품에_대한_여러_좋아요 {

            @Test
            @DisplayName("모든 좋아요가 저장된다")
            void 모든_좋아요가_저장된다() {
                UserId user1Id = new UserId("1");
                UserId user2Id = new UserId("2");
                UserId user3Id = new UserId("3");

                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(productId, user1Id, currentTime),
                        new ProductLikeCache(productId, user2Id, currentTime + 100),
                        new ProductLikeCache(productId, user3Id, currentTime + 200)
                );

                productLikeSynchronizer.sync(cachedLikes, List.of());

                List<ProductLike> savedLikes = productLikeRepository.findAll();
                assertThat(savedLikes)
                        .as("모든 사용자의 좋아요가 저장되어야 함")
                        .hasSize(3);
            }
        }
    }
}
