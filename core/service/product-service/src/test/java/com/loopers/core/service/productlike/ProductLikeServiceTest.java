package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.productlike.command.ProductLikeCommand;
import com.loopers.core.service.productlike.command.ProductUnlikeCommand;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

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
    private ProductLikeRepository productLikeRepository;

    @Nested
    @DisplayName("상품 좋아요 등록 시")
    class 상품_좋아요_등록_시 {

        private BrandId brandId;
        private String productId;
        private String userIdentifier;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(Brand.create(
                    new BrandName("Apple"),
                    new BrandDescription("Apple products")
            ));
            brandId = brand.getId();

            Product product = productRepository.save(Product.create(
                    brandId,
                    new ProductName("MacBook Pro"),
                    new ProductPrice(new BigDecimal("1500000")),
                    new ProductStock(100_000L)
            ));
            productId = product.getId().value();

            User user = userRepository.save(User.create(
                    new UserIdentifier("user123"),
                    new UserEmail("user@example.com"),
                    new UserBirthDay(LocalDate.of(1990, 1, 1)),
                    UserGender.MALE
            ));
            userIdentifier = user.getIdentifier().value();
        }

        @Nested
        @DisplayName("상품이 존재하고 좋아요를 누른 적이 없는 경우")
        class 상품이_존재하고_좋아요를_누른_적이_없는_경우 {

            @Test
            @DisplayName("좋아요가 저장되고 상품의 좋아요 수가 1 증가한다.")
            void 좋아요가_저장되고_상품의_좋아요_수가_1_증가한다() {
                ProductLikeCommand command = new ProductLikeCommand(userIdentifier, productId);
                Product productBefore = productRepository.getById(new ProductId(productId));
                long likeCountBefore = productBefore.getLikeCount().value();

                productLikeService.like(command);

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertSoftly(softly -> {
                    softly.assertThat(productAfter.getLikeCount().value())
                            .as("상품의 좋아요 수가 1 증가해야 함")
                            .isEqualTo(likeCountBefore + 1);
                });
            }
        }

        @Nested
        @DisplayName("하나의 사용자가 여러번 동시에 좋아요를 누른 경우")
        class 하나의_사용자가_여러번_동시에_좋아요를_누른_경우 {

            @Test
            @DisplayName("1번만 증가한다.")
            void 동시에_하나의_사용자가_좋아요를_누르면_1번만_증가한다() throws InterruptedException {
                int requestCount = 100;
                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> productLikeService.like(new ProductLikeCommand(userIdentifier, productId))
                );

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertThat(productAfter.getLikeCount().value()).isEqualTo(1L);
            }
        }

        @Nested
        @DisplayName("동시에 여러 사용자가 좋아요를 누른 경우")
        class 동시에_여러_사용자가_좋아요를_누른_경우 {

            @Test
            @DisplayName("사용자 수 만큼 좋아요 수가 증가한다.")
            void 사용자_수_만큼_좋아요_수가_증가한다() throws InterruptedException {
                int requestCount = 100;

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> {
                            User user = userRepository.save(User.create(
                                    new UserIdentifier("user_" + index),
                                    new UserEmail("user" + index + "@example.com"),
                                    new UserBirthDay(LocalDate.of(1990, 1, 1)),
                                    UserGender.MALE
                            ));
                            productLikeService.like(new ProductLikeCommand(user.getIdentifier().value(), productId));
                        }
                );

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertThat(productAfter.getLikeCount().value()).isEqualTo(requestCount);
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
            @DisplayName("아무것도 하지 않는다.")
            void 아무것도_하지_않는다() {
                ProductLikeCommand secondCommand = new ProductLikeCommand(userIdentifier, productId);
                Product productBefore = productRepository.getById(new ProductId(productId));
                long likeCountBefore = productBefore.getLikeCount().value();

                productLikeService.like(secondCommand);

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertSoftly(softly -> {
                    softly.assertThat(productAfter.getLikeCount().value())
                            .as("좋아요 수가 변하지 않아야 함")
                            .isEqualTo(likeCountBefore);
                });
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
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
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                ProductLikeCommand command = new ProductLikeCommand("not-exist", "1");

                assertThatThrownBy(() -> productLikeService.like(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("사용자");
            }
        }

    }

    @Nested
    @DisplayName("상품 좋아요 취소 시")
    class 상품_좋아요_취소_시 {

        private BrandId brandId;
        private String productId;
        private String userIdentifier;

        @Nested
        @DisplayName("상품이 존재하고 이미 좋아요를 누른 경우")
        class 상품이_존재하고_이미_좋아요를_누른_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(Brand.create(
                        new BrandName("Apple"),
                        new BrandDescription("Apple products")
                ));
                brandId = brand.getId();

                Product product = productRepository.save(Product.create(
                        brandId,
                        new ProductName("MacBook Pro"),
                        new ProductPrice(new BigDecimal("1500000")),
                        new ProductStock(100_000L)
                ));
                productId = product.getId().value();

                User user = userRepository.save(User.create(
                        new UserIdentifier("user123"),
                        new UserEmail("user@example.com"),
                        new UserBirthDay(LocalDate.of(1990, 1, 1)),
                        UserGender.MALE
                ));
                userIdentifier = user.getIdentifier().value();

                ProductLikeCommand likeCommand = new ProductLikeCommand(userIdentifier, productId);
                productLikeService.like(likeCommand);
            }

            @Test
            @DisplayName("좋아요가 삭제되고 상품의 좋아요 수가 1 감소한다.")
            void 좋아요가_삭제되고_상품의_좋아요_수가_1_감소한다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);

                productLikeService.unlike(command);

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertSoftly(softly -> {
                    softly.assertThat(productAfter.getLikeCount().value())
                            .as("상품의 좋아요 수가 1 감소해야 함")
                            .isEqualTo(0L);
                });
            }
        }

        @Nested
        @DisplayName("동시에 하나의 사용자가 여러번 취소하면")
        class 동시에_하나의_사용자가_여러번_취소하면 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(Brand.create(
                        new BrandName("Apple"),
                        new BrandDescription("Apple products")
                ));
                brandId = brand.getId();

                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field("brandId"), brandId)
                                .set(field("id"), ProductId.empty())
                                .set(field("likeCount"), new ProductLikeCount(100L))
                                .create()
                );
                productId = product.getId().value();

                User user = userRepository.save(User.create(
                        new UserIdentifier("user123"),
                        new UserEmail("user@example.com"),
                        new UserBirthDay(LocalDate.of(1990, 1, 1)),
                        UserGender.MALE
                ));
                userIdentifier = user.getIdentifier().value();

                productLikeRepository.save(
                        ProductLike.create(
                                user.getUserId(),
                                product.getId()
                        )
                );
            }

            @Test
            @DisplayName("동시에 하나의 사용자가 여러번 취소하면 1만 감소한다.")
            void 동시에_하나의_사용자가_여러번_취소하면_1만_감소한다() throws InterruptedException {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);

                int requestCount = 100;
                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> productLikeService.unlike(command)
                );

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertThat(productAfter.getLikeCount().value()).isEqualTo(99L);
            }
        }

        @Nested
        @DisplayName("동시에 여러 사용자가 취소하면")
        class 동시에_여러_사용자가_취소하면 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(Brand.create(
                        new BrandName("Apple"),
                        new BrandDescription("Apple products")
                ));
                brandId = brand.getId();

                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field("brandId"), brandId)
                                .set(field("id"), ProductId.empty())
                                .set(field("likeCount"), new ProductLikeCount(100L))
                                .create()
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("사용자의 수만큼 좋아요 수가 감소한다.")
            void 사용자의_수만큼_좋아요_수가_감소한다() throws InterruptedException {
                int requestCount = 100;

                ConcurrencyTestUtil.executeInParallelWithoutResult(
                        requestCount,
                        index -> {
                            User user = userRepository.save(User.create(
                                    new UserIdentifier("user_" + index),
                                    new UserEmail("user" + index + "@example.com"),
                                    new UserBirthDay(LocalDate.of(1990, 1, 1)),
                                    UserGender.MALE
                            ));
                            productLikeRepository.save(
                                    ProductLike.create(
                                            user.getUserId(),
                                            new ProductId(productId)
                                    )
                            );
                            productLikeService.unlike(new ProductUnlikeCommand(user.getIdentifier().value(), productId));
                        }
                );
                Product product = productRepository.getById(new ProductId(productId));
                assertThat(product.getLikeCount().value()).isEqualTo(0L);
            }
        }

        @Nested
        @DisplayName("상품이 존재하고 좋아요를 누른 적이 없는 경우")
        class 상품이_존재하고_좋아요를_누른_적이_없는_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(Brand.create(
                        new BrandName("Apple"),
                        new BrandDescription("Apple products")
                ));
                brandId = brand.getId();

                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field("brandId"), brandId)
                                .set(field("id"), ProductId.empty())
                                .set(field("likeCount"), new ProductLikeCount(100L))
                                .create()
                );
                productId = product.getId().value();
                User user = userRepository.save(User.create(
                        new UserIdentifier("user123"),
                        new UserEmail("user@example.com"),
                        new UserBirthDay(LocalDate.of(1990, 1, 1)),
                        UserGender.MALE
                ));
                userIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("아무것도 하지 않는다.")
            void 아무것도_하지_않는다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand(userIdentifier, productId);
                Product productBefore = productRepository.getById(new ProductId(productId));
                long likeCountBefore = productBefore.getLikeCount().value();

                productLikeService.unlike(command);

                Product productAfter = productRepository.getById(new ProductId(productId));
                assertSoftly(softly -> {
                    softly.assertThat(productAfter.getLikeCount().value())
                            .as("좋아요 수가 변하지 않아야 함")
                            .isEqualTo(likeCountBefore);
                });
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(User.create(
                        new UserIdentifier("user123"),
                        new UserEmail("user@example.com"),
                        new UserBirthDay(LocalDate.of(1990, 1, 1)),
                        UserGender.MALE
                ));
                userIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("NotFoundException이 던져진다.")
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

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                ProductUnlikeCommand command = new ProductUnlikeCommand("not-exist", productId);

                assertThatThrownBy(() -> productLikeService.unlike(command))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("사용자");
            }
        }

    }
}
