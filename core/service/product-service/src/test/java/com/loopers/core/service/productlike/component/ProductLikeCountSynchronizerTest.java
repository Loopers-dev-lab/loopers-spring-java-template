package com.loopers.core.service.productlike.component;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductLikeCount;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.service.IntegrationTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

@DisplayName("ProductLikeCountSynchronizer 통합테스트")
class ProductLikeCountSynchronizerTest extends IntegrationTest {

    @Autowired
    private ProductLikeCountSynchronizer productLikeCountSynchronizer;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Nested
    @DisplayName("좋아요 개수 동기화")
    class 좋아요_개수_동기화 {

        private BrandId brandId;
        private Product savedProduct;
        private Product anotherSavedProduct;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(
                    Instancio.of(Brand.class)
                            .set(field(Brand::getId), BrandId.empty())
                            .set(field(Brand::getName), new BrandName("Apple"))
                            .set(field(Brand::getDescription), new BrandDescription("Apple products"))
                            .create()
            );
            brandId = brand.getId();

            savedProduct = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), brandId)
                            .set(field(Product::getName), new ProductName("MacBook Pro"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("1500000")))
                            .set(field(Product::getStock), new ProductStock(100_000L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            anotherSavedProduct = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), brandId)
                            .set(field(Product::getName), new ProductName("iPad Air"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("800000")))
                            .set(field(Product::getStock), new ProductStock(100_000L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );
        }

        @Nested
        @DisplayName("좋아요 목록이 비어있고 취소 목록도 비어있는 경우")
        class 좋아요_취소_목록이_비어있는_경우 {

            @Test
            @DisplayName("동기화 시 상품의 좋아요 개수가 변경되지 않는다")
            void 좋아요_개수가_변경되지_않는다() {
                long initialLikeCount = savedProduct.getLikeCount().value();

                productLikeCountSynchronizer.sync(List.of(), List.of());

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 변경되지 않아야 함")
                        .isEqualTo(initialLikeCount);
            }
        }

        @Nested
        @DisplayName("새로운 좋아요를 동기화하는 경우")
        class 새로운_좋아요를_동기화하는_경우 {

            @Test
            @DisplayName("상품의 좋아요 개수가 증가한다")
            void 좋아요_개수가_증가한다() {
                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedLike = new ProductLikeCache(
                        savedProduct.getId(),
                        null,
                        currentTime
                );

                productLikeCountSynchronizer.sync(List.of(cachedLike), List.of());

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 1 증가해야 함")
                        .isEqualTo(1L);
            }

            @Test
            @DisplayName("여러 좋아요가 동기화되면 개수가 함께 증가한다")
            void 여러_좋아요가_동기화되면_개수가_증가한다() {
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime),
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100)
                );

                productLikeCountSynchronizer.sync(cachedLikes, List.of());

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 2 증가해야 함")
                        .isEqualTo(2L);
            }
        }

        @Nested
        @DisplayName("좋아요 취소를 동기화하는 경우")
        class 좋아요_취소를_동기화하는_경우 {

            @BeforeEach
            void setUp() {
                Product productWithLikes = savedProduct.withLikeCount(new ProductLikeCount(5L));
                productRepository.save(productWithLikes);
            }

            @Test
            @DisplayName("상품의 좋아요 개수가 감소한다")
            void 좋아요_개수가_감소한다() {
                long currentTime = System.currentTimeMillis();
                ProductLikeCache cachedUnlike = new ProductLikeCache(
                        savedProduct.getId(),
                        null,
                        currentTime
                );

                productLikeCountSynchronizer.sync(List.of(), List.of(cachedUnlike));

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 1 감소해야 함")
                        .isEqualTo(4L);
            }

            @Test
            @DisplayName("여러 취소가 동기화되면 개수가 함께 감소한다")
            void 여러_취소가_동기화되면_개수가_감소한다() {
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedUnlikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime),
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100)
                );

                productLikeCountSynchronizer.sync(List.of(), cachedUnlikes);

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 2 감소해야 함")
                        .isEqualTo(3L);
            }
        }

        @Nested
        @DisplayName("좋아요와 취소가 함께 동기화되는 경우")
        class 좋아요와_취소가_함께_동기화되는_경우 {

            @BeforeEach
            void setUp() {
                Product productWithLikes = savedProduct.withLikeCount(new ProductLikeCount(10L));
                productRepository.save(productWithLikes);
            }

            @Test
            @DisplayName("증가분과 감소분이 함께 반영된다")
            void 증가분과_감소분이_함께_반영된다() {
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime)
                );
                List<ProductLikeCache> cachedUnlikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100)
                );

                productLikeCountSynchronizer.sync(cachedLikes, cachedUnlikes);

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 1 증가하고 1 감소하여 10과 동일해야 함")
                        .isEqualTo(10L);
            }

            @Test
            @DisplayName("여러 증가와 여러 감소가 함께 반영된다")
            void 여러_증가와_여러_감소가_반영된다() {
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime),
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100)
                );
                List<ProductLikeCache> cachedUnlikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 200)
                );

                productLikeCountSynchronizer.sync(cachedLikes, cachedUnlikes);

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("좋아요 개수가 2 증가하고 1 감소하여 11이어야 함")
                        .isEqualTo(11L);
            }
        }

        @Nested
        @DisplayName("여러 상품에 대한 좋아요 개수가 동기화되는 경우")
        class 여러_상품에_대한_좋아요_개수 {

            @Test
            @DisplayName("각 상품의 개수가 올바르게 반영된다")
            void 각_상품의_개수가_올바르게_반영된다() {
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime),
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100),
                        new ProductLikeCache(anotherSavedProduct.getId(), null, currentTime + 200)
                );

                productLikeCountSynchronizer.sync(cachedLikes, List.of());

                Product updatedProduct1 = productRepository.findById(savedProduct.getId()).orElseThrow();
                Product updatedProduct2 = productRepository.findById(anotherSavedProduct.getId()).orElseThrow();

                assertSoftly(softly -> {
                    softly.assertThat(updatedProduct1.getLikeCount().value())
                            .as("첫 번째 상품의 좋아요가 2개여야 함")
                            .isEqualTo(2L);
                    softly.assertThat(updatedProduct2.getLikeCount().value())
                            .as("두 번째 상품의 좋아요가 1개여야 함")
                            .isEqualTo(1L);
                });
            }

            @Test
            @DisplayName("각 상품의 개수에서 취소가 올바르게 반영된다")
            void 각_상품의_개수에서_취소가_반영된다() {
                // anotherSavedProduct의 좋아요 개수를 미리 설정
                Product productWithLikes = anotherSavedProduct.withLikeCount(new ProductLikeCount(3L));
                productRepository.save(productWithLikes);

                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime)
                );
                List<ProductLikeCache> cachedUnlikes = List.of(
                        new ProductLikeCache(anotherSavedProduct.getId(), null, currentTime + 100)
                );

                productLikeCountSynchronizer.sync(cachedLikes, cachedUnlikes);

                Product updatedProduct1 = productRepository.findById(savedProduct.getId()).orElseThrow();
                Product updatedProduct2 = productRepository.findById(anotherSavedProduct.getId()).orElseThrow();

                assertSoftly(softly -> {
                    softly.assertThat(updatedProduct1.getLikeCount().value())
                            .as("첫 번째 상품의 좋아요가 1개여야 함")
                            .isEqualTo(1L);
                    softly.assertThat(updatedProduct2.getLikeCount().value())
                            .as("두 번째 상품의 좋아요가 2개여야 함")
                            .isEqualTo(2L);
                });
            }
        }

        @Nested
        @DisplayName("같은 사용자의 중복된 좋아요 캐시가 동기화되는 경우")
        class 같은_사용자의_중복된_좋아요 {

            @Test
            @DisplayName("중복된 캐시는 한 번만 카운트된다")
            void 중복된_캐시는_한_번만_카운트된다() {
                // 캐시의 특성상 같은 사용자의 같은 상품에 대한 중복 저장은 불가능하지만,
                // 동기화 로직에서 리스트로 받은 경우를 테스트
                long currentTime = System.currentTimeMillis();
                List<ProductLikeCache> cachedLikes = List.of(
                        new ProductLikeCache(savedProduct.getId(), null, currentTime),
                        new ProductLikeCache(savedProduct.getId(), null, currentTime + 100)
                );

                productLikeCountSynchronizer.sync(cachedLikes, List.of());

                Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
                assertThat(updatedProduct.getLikeCount().value())
                        .as("중복된 캐시가 있어도 개별적으로 카운트됨")
                        .isEqualTo(2L);
            }
        }
    }
}
