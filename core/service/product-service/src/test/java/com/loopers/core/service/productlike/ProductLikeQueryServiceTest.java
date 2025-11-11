package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.productlike.query.GetLikeProductsListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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

        private String savedUserId;
        private BrandId savedBrandId;
        private BrandId otherBrandId;

        @BeforeEach
        void setUp() {
            User user = userRepository.save(User.create(
                    new UserIdentifier("testUser"),
                    new UserEmail("test@loopers.com"),
                    new UserBirthDay(LocalDate.of(2000, 1, 1)),
                    UserGender.MALE
            ));
            savedUserId = user.getUserId().value();

            Brand brand = brandRepository.save(Brand.create(
                    new BrandName("Apple"),
                    new BrandDescription("Apple products")
            ));
            savedBrandId = brand.getBrandId();

            Brand otherBrand = brandRepository.save(Brand.create(
                    new BrandName("Samsung"),
                    new BrandDescription("Samsung products")
            ));
            otherBrandId = otherBrand.getBrandId();
        }

        @Nested
        @DisplayName("사용자가 좋아요한 상품이 존재하는 경우")
        class 사용자가_좋아요한_상품이_존재하는_경우 {

            @BeforeEach
            void setUp() {
                Product product1 = productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("MacBook Pro"),
                                new ProductPrice(new BigDecimal(1_300_000))
                        )
                );
                Product product2 = productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("iPad Air"),
                                new ProductPrice(new BigDecimal(800_000))
                        )
                );
                Product product3 = productRepository.save(
                        Product.create(
                                otherBrandId,
                                new ProductName("Galaxy S24"),
                                new ProductPrice(new BigDecimal(1_100_000))
                        )
                );

                productLikeRepository.save(ProductLike.create(
                        new UserId(savedUserId),
                        product1.getProductId()
                ));
                productLikeRepository.save(ProductLike.create(
                        new UserId(savedUserId),
                        product2.getProductId()
                ));
                productLikeRepository.save(ProductLike.create(
                        new UserId(savedUserId),
                        product3.getProductId()
                ));
            }

            @Test
            @DisplayName("사용자의 좋아요 상품 리스트가 조회된다.")
            void 사용자의_좋아요_상품_리스트가_조회된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserId,
                        savedBrandId.value(),
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
                    softly.assertThat(result.getTotalElements()).isEqualTo(2);
                });
            }

            @Test
            @DisplayName("브랜드별로 필터링된 좋아요 상품만 조회된다.")
            void 브랜드별로_필터링된_좋아요_상품만_조회된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserId,
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                LikeProductListView result = productLikeQueryService.getLikeProductsListView(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getTotalElements()).isEqualTo(2);
                    softly.assertThat(result.getItems())
                            .allMatch(item -> item.getBrandId().value().equals(savedBrandId.value()),
                                    "모든 상품이 특정 브랜드에 속해야 함");
                });
            }

            @Test
            @DisplayName("생성일시 오름차순으로 정렬된다.")
            void 생성일시_오름차순으로_정렬된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserId,
                        savedBrandId.value(),
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
                        savedUserId,
                        savedBrandId.value(),
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
                        savedUserId,
                        savedBrandId.value(),
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

            @Test
            @DisplayName("빈 리스트가 반환된다.")
            void 빈_리스트가_반환된다() {
                GetLikeProductsListQuery query = new GetLikeProductsListQuery(
                        savedUserId,
                        savedBrandId.value(),
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
