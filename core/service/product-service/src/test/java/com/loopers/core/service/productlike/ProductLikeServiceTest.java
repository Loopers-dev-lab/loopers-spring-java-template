package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.BrandFixture;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.repository.ProductLikeCacheRepository;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductLikeCount;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserFixture;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.productlike.command.ProductLikeCommand;
import com.loopers.core.service.productlike.command.ProductUnlikeCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("ProductLikeService 테스트")
class ProductLikeServiceTest extends IntegrationTest {

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductLikeCacheRepository productLikeCacheRepository;

    @Nested
    @DisplayName("상품 좋아요 등록")
    class 상품_좋아요_등록 {

        private BrandId brandId;
        private String productId;
        private String userIdentifier;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
            brandId = brand.getId();

            Product product = productRepository.save(
                    ProductFixture.createWith(
                            brandId,
                            new ProductName("MacBook Pro"),
                            new ProductPrice(new BigDecimal("1500000")),
                            new ProductStock(100_000L)
                    )
            );
            productId = product.getId().value();

            User user = userRepository.save(
                    UserFixture.createWith("user123", "user@example.com")
            );
            userIdentifier = user.getIdentifier().value();
        }

        @Nested
        @DisplayName("상품이 존재하고 좋아요를 누른 적이 없는 경우")
        class 상품이_존재하고_좋아요를_누른_적이_없는_경우 {

            @Test
            @DisplayName("좋아요가 캐시에 저장된다")
            void 좋아요가_캐시에_저장된다() {
                ProductLikeCommand command = new ProductLikeCommand(userIdentifier, productId);

                productLikeService.like(command);

                // 캐시에 좋아요가 저장되었는지 확인
                long currentTime = System.currentTimeMillis();
                var likes = productLikeCacheRepository.getLikesSinceLastSync(0, currentTime);
                assertThat(likes).isNotEmpty();
                assertSoftly(softly -> {
                    softly.assertThat(likes)
                            .as("캐시에 좋아요가 저장되어야 함")
                            .anyMatch(cache -> cache.productId().value().equals(productId));
                });
            }
        }

        @Nested
        @DisplayName("하나의 사용자가 여러번 동시에 좋아요를 누른 경우")
        class 하나의_사용자가_여러번_동시에_좋아요를_누른_경우 {

            @Test
            @DisplayName("캐시의 특성상 1개로 기록되고 타임스탬프만 업데이트된다")
            void 캐시의_특성상_1개로_기록된다() throws InterruptedException {
                int requestCount = 100;
                long beforeTime = System.currentTimeMillis();

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> productLikeService.like(new ProductLikeCommand(userIdentifier, productId))
                );

                long afterTime = System.currentTimeMillis() + 1000; // 마진 추가
                var likes = productLikeCacheRepository.getLikesSinceLastSync(beforeTime, afterTime);

                // 캐시는 같은 사용자의 같은 상품에 대해 1개만 저장하고 타임스탬프만 업데이트함
                assertThat(likes)
                        .as("캐시의 특성상 같은 사용자는 1개로만 저장됨")
                        .hasSize(1);
            }
        }

        @Nested
        @DisplayName("동시에 여러 사용자가 좋아요를 누른 경우")
        class 동시에_여러_사용자가_좋아요를_누른_경우 {

            @Test
            @DisplayName("모든 좋아요가 캐시에 저장된다")
            void 모든_좋아요가_캐시에_저장된다() throws InterruptedException {
                int requestCount = 100;
                long beforeTime = System.currentTimeMillis();

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> {
                            User user = userRepository.save(
                                    UserFixture.createWith("user" + index, "user_" + index + "@example.com")
                            );
                            productLikeService.like(new ProductLikeCommand(user.getIdentifier().value(), productId));
                        }
                );

                long afterTime = System.currentTimeMillis() + 1000; // 마진 추가
                var likes = productLikeCacheRepository.getLikesSinceLastSync(beforeTime, afterTime);

                assertThat(likes)
                        .as("캐시에 사용자 수만큼 좋아요가 저장되어야 함")
                        .hasSize(requestCount);
            }
        }

        @Nested
        @DisplayName("상품이 존재하고 이미 좋아요를 누른 경우")
        class 상품이_존재하고_이미_좋아요를_누른_경우 {

            @BeforeEach
            void setUp() {
                ProductLikeCommand firstCommand = new ProductLikeCommand(userIdentifier, productId);
                productLikeService.like(firstCommand);
            }

            @Test
            @DisplayName("좋아요를 누르면 캐시에 새로 추가된다")
            void 좋아요를_누르면_캐시에_새로_추가된다() {
                ProductLikeCommand secondCommand = new ProductLikeCommand(userIdentifier, productId);

                productLikeService.like(secondCommand);

                long currentTime = System.currentTimeMillis();
                var likes = productLikeCacheRepository.getLikesSinceLastSync(0, currentTime);

                assertThat(likes)
                        .as("캐시에 좋아요가 저장되어야 함")
                        .isNotEmpty();
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다")
            void NotFoundException이_던져진다() {
                ProductLikeCommand command = new ProductLikeCommand(userIdentifier, "99999");

                assertThatThrownBy(() -> productLikeService.like(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("상품");
            }
        }

        @Nested
        @DisplayName("사용자가 존재하지 않는 경우")
        class 사용자가_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다")
            void NotFoundException이_던져진다() {
                ProductLikeCommand command = new ProductLikeCommand("not-exist", "1");

                assertThatThrownBy(() -> productLikeService.like(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("사용자");
            }
        }

    }

    @Nested
    @DisplayName("상품 좋아요 취소")
    class 상품_좋아요_취소 {

        private BrandId brandId;
        private String productId;
        private String userIdentifier;

        @Nested
        @DisplayName("상품이 존재하고 이미 좋아요를 누른 경우")
        class 상품이_존재하고_이미_좋아요를_누른_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
                brandId = brand.getId();

                Product product = productRepository.save(
                        ProductFixture.createWith(
                                brandId,
                                new ProductName("MacBook Pro"),
                                new ProductPrice(new BigDecimal("1500000")),
                                new ProductStock(100_000L)
                        )
                );
                productId = product.getId().value();

                User user = userRepository.save(
                        UserFixture.createWith("user123", "user@example.com")
                );
                userIdentifier = user.getIdentifier().value();

                ProductLikeCommand likeCommand = new ProductLikeCommand(userIdentifier, productId);
                productLikeService.like(likeCommand);
            }

            @Test
            @DisplayName("좋아요 취소가 캐시에 저장된다")
            void 좋아요_취소가_캐시에_저장된다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);

                productLikeService.unlike(command);

                long currentTime = System.currentTimeMillis();
                var unlikes = productLikeCacheRepository.getUnlikesSinceLastSync(0, currentTime);

                assertThat(unlikes)
                        .as("캐시에 좋아요 취소가 저장되어야 함")
                        .isNotEmpty();
            }
        }

        @Nested
        @DisplayName("동시에 하나의 사용자가 여러번 취소하면")
        class 동시에_하나의_사용자가_여러번_취소하면 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
                brandId = brand.getId();

                Product product = productRepository.save(
                        ProductFixture.createWith(brandId, new ProductLikeCount(100L))
                );
                productId = product.getId().value();

                User user = userRepository.save(
                        UserFixture.createWith("user123", "user@example.com")
                );
                userIdentifier = user.getIdentifier().value();

                // 먼저 좋아요를 캐시에 저장
                productLikeService.like(new ProductLikeCommand(userIdentifier, productId));
            }

            @Test
            @DisplayName("캐시의 특성상 1개로 기록되고 타임스탬프만 업데이트된다")
            void 캐시의_특성상_1개로_기록된다() throws InterruptedException {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);

                int requestCount = 100;
                long beforeTime = System.currentTimeMillis();

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> productLikeService.unlike(command)
                );

                long afterTime = System.currentTimeMillis() + 1000; // 마진 추가
                var unlikes = productLikeCacheRepository.getUnlikesSinceLastSync(beforeTime, afterTime);

                // 캐시는 같은 사용자의 같은 상품에 대해 1개만 저장하고 타임스탬프만 업데이트함
                assertThat(unlikes)
                        .as("캐시의 특성상 같은 사용자는 1개로만 저장됨")
                        .hasSize(1);
            }
        }

        @Nested
        @DisplayName("동시에 여러 사용자가 취소하면")
        class 동시에_여러_사용자가_취소하면 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
                brandId = brand.getId();

                Product product = productRepository.save(
                        ProductFixture.createWith(brandId, new ProductLikeCount(100L))
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("모든 사용자의 취소 요청이 캐시에 저장된다")
            void 모든_사용자의_취소_요청이_캐시에_저장된다() throws InterruptedException {
                int requestCount = 100;
                long beforeTime = System.currentTimeMillis();

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> {
                            User user = userRepository.save(
                                    UserFixture.createWith("user" + index, "user_" + index + "@example.com")
                            );
                            // 먼저 좋아요 캐시에 저장
                            productLikeService.like(new ProductLikeCommand(user.getIdentifier().value(), productId));
                            // 그 다음 취소
                            productLikeService.unlike(new ProductUnlikeCommand(user.getIdentifier().value(), productId));
                        }
                );

                long afterTime = System.currentTimeMillis() + 1000; // 마진 추가
                var unlikes = productLikeCacheRepository.getUnlikesSinceLastSync(beforeTime, afterTime);

                assertThat(unlikes)
                        .as("캐시에 모든 사용자의 취소 요청이 저장되어야 함")
                        .hasSize(requestCount);
            }
        }

        @Nested
        @DisplayName("상품이 존재하고 좋아요를 누른 적이 없는 경우")
        class 상품이_존재하고_좋아요를_누른_적이_없는_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
                brandId = brand.getId();

                Product product = productRepository.save(
                        ProductFixture.createWith(brandId, new ProductLikeCount(100L))
                );
                productId = product.getId().value();
                User user = userRepository.save(
                        UserFixture.createWith("user123", "user@example.com")
                );
                userIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("취소 요청이 캐시에 저장된다")
            void 취소_요청이_캐시에_저장된다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);

                productLikeService.unlike(command);

                long currentTime = System.currentTimeMillis();
                var unlikes = productLikeCacheRepository.getUnlikesSinceLastSync(0, currentTime);

                assertThat(unlikes)
                        .as("캐시에 취소 요청이 저장되어야 함")
                        .isNotEmpty();
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(
                        UserFixture.createWith("user123", "user@example.com")
                );
                userIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("NotFoundException이 던져진다")
            void NotFoundException이_던져진다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, "99999");

                assertThatThrownBy(() -> productLikeService.unlike(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("상품");
            }
        }

        @Nested
        @DisplayName("사용자가 존재하지 않는 경우")
        class 사용자가_존재하지_않는_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(BrandFixture.createWith("Apple"));
                brandId = brand.getId();

                Product product = productRepository.save(
                        ProductFixture.createWith(brandId, new ProductLikeCount(100L))
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("NotFoundException이 던져진다")
            void NotFoundException이_던져진다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand("not-exist", productId);

                assertThatThrownBy(() -> productLikeService.unlike(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("사용자");
            }
        }

    }
}
