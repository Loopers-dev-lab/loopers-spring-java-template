package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.productlike.vo.ProductLikeId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.productlike.query.GetLikeProductsListQuery;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

public class ProductLikeQueryServiceTest extends IntegrationTest {

    @Autowired
    private ProductLikeQueryService productLikeQueryService;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("좋아요 상품 리스트 조회 시")
    class 좋아요_상품_리스트_조회_시 {

        private String savedUserIdentifier;
        private BrandId savedBrandId;

        @Nested
        @DisplayName("사용자가 좋아요한 상품이 존재하는 경우")
        class 사용자가_좋아요한_상품이_존재하는_경우 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), UserIdentifier.create("testUser"))
                                .set(field(User::getEmail), new UserEmail("test@loopers.com"))
                                .create()
                );
                savedUserIdentifier = user.getIdentifier().value();
                savedBrandId = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .set(field(Brand::getName), new BrandName("Test Brand"))
                                .set(field(Brand::getDescription), new BrandDescription("Test Description"))
                                .create()
                ).getId();

                Product product1 = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), savedBrandId)
                                .set(field(Product::getName), new ProductName("MacBook Pro"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(1_300_000)))
                                .set(field(Product::getStock), new ProductStock(10L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                );
                Product product2 = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), savedBrandId)
                                .set(field(Product::getName), new ProductName("iPad Air"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(800_000)))
                                .set(field(Product::getStock), new ProductStock(10L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                );
                Product product3 = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), savedBrandId)
                                .set(field(Product::getName), new ProductName("Galaxy S24"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(1_100_000)))
                                .set(field(Product::getStock), new ProductStock(10L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .set(field(Product::getDeletedAt), DeletedAt.empty())
                                .create()
                );

                productLikeRepository.save(
                        Instancio.of(ProductLike.class)
                                .set(field(ProductLike::getId), ProductLikeId.empty())
                                .set(field(ProductLike::getUserId), user.getId())
                                .set(field(ProductLike::getProductId), product1.getId())
                                .create()
                );
                productLikeRepository.save(
                        Instancio.of(ProductLike.class)
                                .set(field(ProductLike::getId), ProductLikeId.empty())
                                .set(field(ProductLike::getUserId), user.getId())
                                .set(field(ProductLike::getProductId), product2.getId())
                                .create()
                );
                productLikeRepository.save(
                        Instancio.of(ProductLike.class)
                                .set(field(ProductLike::getId), ProductLikeId.empty())
                                .set(field(ProductLike::getUserId), user.getId())
                                .set(field(ProductLike::getProductId), product3.getId())
                                .create()
                );
            }

            @Test
            @DisplayName("사용자의 좋아요 상품 리스트가 조회된다.")
            void 사용자의_좋아요_상품_리스트가_조회된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        null,
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getItems()).isNotEmpty();
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                });
            }

            @Test
            @DisplayName("브랜드별로 필터링된 좋아요 상품만 조회된다.")
            void 브랜드별로_필터링된_좋아요_상품만_조회된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                    softly.assertThat(result.getItems())
                            .allMatch(item -> item.getBrandId().value().equals(savedBrandId.value()),
                                    "모든 상품이 특정 브랜드에 속해야 함");
                });
            }

            @Test
            @DisplayName("생성일시 오름차순으로 정렬된다.")
            void 생성일시_오름차순으로_정렬된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        null,
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item1.getCreatedAt().value().compareTo(item2.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("생성일시 내림차순으로 정렬된다.")
            void 생성일시_내림차순으로_정렬된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        null,
                        "DESC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item2.getCreatedAt().value().compareTo(item1.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("가격을 함께 정렬할 때 유효하다.")
            void 가격을_함께_정렬할_때_유효하다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        null,
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2);
            }
        }

        @Nested
        @DisplayName("사용자가 좋아요한 상품이 없는 경우")
        class 사용자가_좋아요한_상품이_없는_경우 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), UserIdentifier.create("testUser"))
                                .set(field(User::getEmail), new UserEmail("test@loopers.com"))
                                .create()
                );
                savedUserIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("빈 리스트가 반환된다.")
            void 빈_리스트가_반환된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserIdentifier,
                        null,
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getItems()).isEmpty();
                    softly.assertThat(result.getTotalElements()).isZero();
                    softly.assertThat(result.getTotalPages()).isZero();
                    softly.assertThat(result.isHasNext()).isFalse();
                    softly.assertThat(result.isHasPrevious()).isFalse();
                });
            }
        }
    }
}
