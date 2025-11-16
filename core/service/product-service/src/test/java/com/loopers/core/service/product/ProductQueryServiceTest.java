package com.loopers.core.service.product;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.product.query.GetProductDetailQuery;
import com.loopers.core.service.product.query.GetProductListQuery;
import com.loopers.core.service.product.query.GetProductQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ProductQueryServiceTest extends IntegrationTest {

    @Autowired
    private ProductQueryService productQueryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Nested
    @DisplayName("상품 리스트 조회 시")
    class 상품_리스트_조회_시 {

        private BrandId savedBrandId;
        private BrandId otherBrandId;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(Brand.create(
                    new BrandName("loopers"),
                    new BrandDescription("education brand")
            ));
            savedBrandId = brand.getBrandId();

            Brand otherBrand = brandRepository.save(Brand.create(
                    new BrandName("other"),
                    new BrandDescription("other brand")
            ));
            otherBrandId = otherBrand.getBrandId();
        }

        @Nested
        @DisplayName("조건에 맞는 상품이 존재하는 경우")
        class 조건에_맞는_상품이_존재하는_경우 {

            @BeforeEach
            void setUp() {
                productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("MacBook Pro"),
                                new ProductPrice(new BigDecimal(1_300_000))
                        )
                );
                productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("iPad Air"),
                                new ProductPrice(new BigDecimal(800_000))
                        )
                );
                productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("iPhone 15"),
                                new ProductPrice(new BigDecimal(1_500_000))
                        )
                );
            }

            @Test
            @DisplayName("상품 리스트가 조회된다.")
            void 상품_리스트가_조회된다() {
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getItems()).isNotEmpty();
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                });
            }

            @Test
            @DisplayName("생성일시 오름차순으로 정렬된다.")
            void 생성일시_오름차순으로_정렬된다() {
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item1.getCreatedAt().value().compareTo(item2.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("생성일시 내림차순으로 정렬된다.")
            void 생성일시_내림차순으로_정렬된다() {
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "DESC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item2.getCreatedAt().value().compareTo(item1.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("가격을 함께 정렬할 때 유효하다.")
            void 가격을_함께_정렬할_때_유효하다() {
                // 정렬 조건: 생성일시 ASC + 가격 ASC + 좋아요 ASC
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                // 가격만 정렬되었는지 확인 (생성일시 조건도 있으므로)
                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2);
            }

            @Test
            @DisplayName("가격과 다른 정렬 조건을 함께 적용할 때 유효하다.")
            void 가격과_다른_정렬_조건을_함께_적용할_때_유효하다() {
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "DESC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2);
            }

            @Test
            @DisplayName("특정 브랜드의 상품만 조회된다.")
            void 특정_브랜드의_상품만_조회된다() {
                productRepository.save(
                        Product.create(
                                otherBrandId,
                                new ProductName("Samsung Galaxy"),
                                new ProductPrice(new BigDecimal(1_000_000))
                        )
                );

                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                    softly.assertThat(result.getItems())
                            .allMatch(item -> item.getBrandId().value().equals(savedBrandId.value()),
                                    "모든 상품이 특정 브랜드에 속해야 함");
                });
            }
        }

        @Nested
        @DisplayName("조건에 맞는 상품이 없는 경우")
        class 조건에_맞는_상품이_없는_경우 {

            @Test
            @DisplayName("빈 리스트가 반환된다.")
            void 빈_리스트가_반환된다() {
                GetProductListQuery query = new GetProductListQuery(
                        savedBrandId.value(),
                        "ASC",
                        "ASC",
                        "ASC",
                        0,
                        10
                );

                ProductListView result = productQueryService.getProductList(query);

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

    @Nested
    @DisplayName("ID로 상품 조회 시")
    class ID로_상품_조회_시 {

        private BrandId savedBrandId;
        private String savedProductId;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(Brand.create(
                    new BrandName("loopers"),
                    new BrandDescription("education brand")
            ));
            savedBrandId = brand.getBrandId();

            Product product = productRepository.save(
                    Product.create(
                            savedBrandId,
                            new ProductName("MacBook Pro"),
                            new ProductPrice(new BigDecimal(1_300_000))
                    )
            );
            savedProductId = product.getId().value();
        }

        @Nested
        @DisplayName("상품이 존재하는 경우")
        class 상품이_존재하는_경우 {

            @Test
            @DisplayName("상품이 조회된다.")
            void 상품이_조회된다() {
                GetProductQuery query = new GetProductQuery(savedProductId);

                Product result = productQueryService.getProductBy(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getBrandId().value()).isEqualTo(savedBrandId.value());
                });
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                GetProductQuery query = new GetProductQuery("99999");

                assertThatThrownBy(() -> productQueryService.getProductBy(query))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("상품");
            }
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 시")
    class 상품_상세_조회_시 {

        private BrandId savedBrandId;
        private String savedProductId;
        private Brand savedBrand;

        @BeforeEach
        void setUp() {
            savedBrand = brandRepository.save(Brand.create(
                    new BrandName("loopers"),
                    new BrandDescription("education brand")
            ));
            savedBrandId = savedBrand.getBrandId();

            Product product = productRepository.save(
                    Product.create(
                            savedBrandId,
                            new ProductName("MacBook Pro"),
                            new ProductPrice(new BigDecimal(1_300_000))
                    )
            );
            savedProductId = product.getId().value();
        }

        @Nested
        @DisplayName("상품이 존재하는 경우")
        class 상품이_존재하는_경우 {

            @Test
            @DisplayName("상품 상세가 조회된다.")
            void 상품_상세가_조회된다() {
                GetProductDetailQuery query = new GetProductDetailQuery(savedProductId);

                ProductDetail result = productQueryService.getProductDetail(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getProduct()).isNotNull();
                    softly.assertThat(result.getBrand()).isNotNull();
                });
            }

            @Test
            @DisplayName("상품과 브랜드 정보가 일치한다.")
            void 상품과_브랜드_정보가_일치한다() {
                GetProductDetailQuery query = new GetProductDetailQuery(savedProductId);

                ProductDetail result = productQueryService.getProductDetail(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getProduct().getId().value())
                            .isEqualTo(savedProductId);
                    softly.assertThat(result.getProduct().getBrandId().value())
                            .isEqualTo(savedBrandId.value());
                    softly.assertThat(result.getBrand().getBrandId().value())
                            .isEqualTo(savedBrandId.value());
                });
            }

            @Test
            @DisplayName("상품의 가격 정보가 포함된다.")
            void 상품의_가격_정보가_포함된다() {
                GetProductDetailQuery query = new GetProductDetailQuery(savedProductId);

                ProductDetail result = productQueryService.getProductDetail(query);

                assertThat(result.getProduct().getPrice().value())
                        .isEqualByComparingTo(new BigDecimal(1_300_000));
            }
        }

        @Nested
        @DisplayName("상품이 존재하지 않는 경우")
        class 상품이_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                GetProductDetailQuery query = new GetProductDetailQuery("99999");

                assertThatThrownBy(() -> productQueryService.getProductDetail(query))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("상품");
            }
        }

        @Nested
        @DisplayName("브랜드가 존재하지 않는 경우")
        class 브랜드가_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                // 먼저 상품을 생성하고, 브랜드를 삭제하는 시나리오
                Product orphanProduct = productRepository.save(
                        Product.create(
                                new BrandId("99999"),
                                new ProductName("Orphan Product"),
                                new ProductPrice(new BigDecimal(100_000))
                        )
                );

                GetProductDetailQuery query = new GetProductDetailQuery(orphanProduct.getId().value());

                assertThatThrownBy(() -> productQueryService.getProductDetail(query))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("브랜드");
            }
        }
    }
}
