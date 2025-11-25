package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeEntity;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@SpringBootTest
@DisplayName("Product 통합 테스트")
public class ProductIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeRepository likeRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("상품 저장")
    class ProductSaveTest {

        @Test
        @DisplayName("새로운 상품 등록 시 상품이 저장된다")
        void save_product_with_brand() {
            // given
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Nike", "Just do it");

            // when
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Air Max",
                    "편안한 운동화",
                    new BigDecimal("120000"),
                    50
            );

            // then
            assertThat(product.getId()).isNotNull();
            assertThat(product.getName()).isEqualTo("Air Max");
            assertThat(product.getBrandId()).isEqualTo(brand.getId());
        }
    }

    @Nested
    @DisplayName("상품 목록 조회")
    class ProductListTest {

        @Test
        @DisplayName("상품 목록을 페이징하여 조회할 수 있다")
        void get_product_pagination() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            Pageable pageable = PageRequest.of(0, 5);
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(null, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(5);
            assertThat(productInfos.getTotalElements()).isEqualTo(10);

            ProductInfo firstProduct = productInfos.getContent().get(0);
            assertThat(firstProduct.name()).isNotNull();
            assertThat(firstProduct.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(firstProduct.likeCount()).isNotNull();
        }

        @Test
        @DisplayName("브랜드 ID로 상품을 필터링하여 조회할 수 있다")
        void filter_products_by_brand() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            Pageable pageable = PageRequest.of(0, 25);
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(1L, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(5);
            assertThat(productInfos.getTotalElements()).isEqualTo(5);

            productInfos.getContent().forEach(productInfo -> {
                assertThat(productInfo.brandId()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("상품이 없는 경우 빈 목록을 반환한다")
        void return_empty_list_when_no_products() {
            Pageable pageable = PageRequest.of(0, 25);
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(1L, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(0);
            assertThat(productInfos.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("최신순으로 상품을 정렬하여 조회할 수 있다")
        void get_products_sorted_by_latest() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5); // 1개 브랜드, 5개 상품 생성

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(null, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(5);
            assertThat(productInfos.getTotalElements()).isEqualTo(5);

            // 최신순 정렬 검증 (생성일시 내림차순)
            List<java.time.ZonedDateTime> createdAts = productInfos.getContent().stream()
                    .map(ProductInfo::createdAt)  // ProductInfo에 생성일시 필드가 있다고 가정
                    .toList();
            assertThat(createdAts).isSortedAccordingTo(Comparator.reverseOrder());
        }

        @Test
        @DisplayName("존재하지 않는 브랜드로 상품을 필터링하면 빈 목록을 반환한다")
        void return_empty_list_when_filtering_by_non_existent_brand() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            Pageable pageable = PageRequest.of(0, 25);
            ProductSearchFilter productSearchFilter = new ProductSearchFilter(9L, null, pageable);

            // when
            Page<ProductInfo> productInfos = productFacade.getProducts(productSearchFilter);

            // then
            assertThat(productInfos).isNotNull();
            assertThat(productInfos.getContent()).hasSize(0);
            assertThat(productInfos.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("상품 상세 조회")
    class ProductDetailTest {

        @Test
        @DisplayName("상품 상세를 조회할 수 있다")
        void get_product_detail_success() {
            // given
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5); // 2개 브랜드, 각 브랜드당 5개 상품 생성

            // when
            ProductDetailInfo productDetail = productFacade.getProductDetail(1L, null);

            // then
            assertThat(productDetail).isNotNull();
            assertThat(productDetail.name()).isNotNull();
            assertThat(productDetail.price().originPrice()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(productDetail.brand()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 상품 상세 조회 시 ProductNotFoundException이 발생한다")
        void throw_exception_when_product_not_found() {
            // given
            Long nonExistentId = 999L;

            // when & then
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                    com.loopers.support.error.CoreException.class,
                    () -> productFacade.getProductDetail(nonExistentId, null)
            ).getErrorType()).isEqualTo(ErrorType.NOT_FOUND_PRODUCT);
        }

        @Test
        @DisplayName("상품 상세 조회 시 존재하지 않는 브랜드인 경우 예외가 발생한다")
        void throw_exception_when_product_has_non_existent_brand() {
            // given
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);

            // 브랜드 삭제 (소프트 삭제)
            brand.delete();
            brandRepository.save(brand);

            // when & then
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                    com.loopers.support.error.CoreException.class,
                    () -> productFacade.getProductDetail(product.getId(), null)
            ).getErrorType()).isEqualTo(ErrorType.NOT_FOUND_BRAND); // 적절한 에러 타입으로 변경
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 - 좋아요 정보 포함")
    class ProductDetailWithLikeInfoTest {

        @Test
        @DisplayName("로그인한 사용자가 좋아요한 상품 조회 시 isLiked가 true다")
        void should_return_is_liked_true_when_user_liked_product() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 브랜드와 상품 생성
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Test Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // Given: 좋아요 등록
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // When: 상품 상세 조회 (사용자 정보 포함)
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    userInfo.username()
            );

            // Then: 좋아요 여부 및 카운트 확인
            assertThat(result.isLiked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("로그인한 사용자가 좋아요하지 않은 상품 조회 시 isLiked가 false다")
        void should_return_is_liked_false_when_user_not_liked_product() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 브랜드와 상품 생성 (좋아요는 등록하지 않음)
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Test Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // When: 상품 상세 조회 (사용자 정보 포함)
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    userInfo.username()
            );

            // Then: 좋아요 여부 및 카운트 확인
            assertThat(result.isLiked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("비로그인 사용자가 상품 조회 시 isLiked가 false이다")
        void should_return_is_liked_false_when_anonymous_user() {
            // Given: 브랜드와 상품 생성
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Test Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // When: 비로그인 상태로 상품 조회
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    null  // 비로그인
            );

            // Then: 좋아요 여부는 false, 카운트는 0
            assertThat(result.isLiked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("여러 사용자가 좋아요한 상품의 좋아요 수가 정확히 표시된다")
        void should_show_correct_like_count_when_multiple_users_liked() {
            // Given: 여러 사용자 생성
            UserRegisterCommand command1 = UserTestFixture.createUserCommand(
                    "user1", "user1@example.com", "1990-01-01", com.loopers.domain.user.Gender.MALE
            );
            UserRegisterCommand command2 = UserTestFixture.createUserCommand(
                    "user2", "user2@example.com", "1990-01-01", com.loopers.domain.user.Gender.FEMALE
            );
            UserRegisterCommand command3 = UserTestFixture.createUserCommand(
                    "user3", "user3@example.com", "1990-01-01", com.loopers.domain.user.Gender.MALE
            );

            UserInfo user1 = userFacade.registerUser(command1);
            UserInfo user2 = userFacade.registerUser(command2);
            UserInfo user3 = userFacade.registerUser(command3);

            // Given: 브랜드와 상품 생성
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Popular Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // Given: 세 명의 사용자가 좋아요 등록
            likeFacade.upsertLike(user1.username(), product.getId());
            likeFacade.upsertLike(user2.username(), product.getId());
            likeFacade.upsertLike(user3.username(), product.getId());

            // When: user1이 상품 조회
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    user1.username()
            );

            // Then: user1은 좋아요 했고, 총 좋아요 수는 3
            assertThat(result.isLiked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("좋아요 취소 후 상품 조회 시 isLiked가 false이고 카운트가 감소한다")
        void should_return_is_liked_false_and_decreased_count_after_unlike() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 브랜드와 상품 생성
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Test Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // Given: 좋아요 등록 후 취소
            likeFacade.upsertLike(userInfo.username(), product.getId());
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // When: 상품 상세 조회
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    userInfo.username()
            );

            // Then: 좋아요 취소 상태 확인
            assertThat(result.isLiked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("삭제된 좋아요는 isLiked false로 표시되고 카운트에 포함되지 않는다")
        void should_not_count_deleted_likes() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 브랜드와 상품 생성
            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Test Description");
            ProductEntity product = ProductTestFixture.createAndSave(
                    productRepository,
                    brand,
                    "Test Product",
                    "Product Description",
                    new BigDecimal("10000"),
                    100
            );

            // Given: 삭제된 좋아요 엔티티 직접 생성
            LikeEntity deletedLike = LikeEntity.createEntity(userInfo.id(), product.getId());
            deletedLike.delete();
            likeRepository.save(deletedLike);

            // When: 상품 상세 조회
            ProductDetailInfo result = productFacade.getProductDetail(
                    product.getId(),
                    userInfo.username()
            );

            // Then: 삭제된 좋아요는 카운트되지 않음
            assertThat(result.isLiked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
        }
    }
}
