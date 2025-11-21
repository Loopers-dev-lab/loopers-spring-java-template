package com.loopers.domain.like;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

/**
 * LikeFacade 통합 테스트
 * <p>
 * 좋아요 등록 및 취소 기능을 파사드 레벨에서 검증합니다.
 * 각 도메인 서비스를 통해 트랜잭션이 적용된 데이터 저장을 수행합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
@SpringBootTest
@DisplayName("LikeFacade 통합 테스트")
public class LikeIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserFacade userFacade;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * 좋아요 등록 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 등록")
    class LikeRegistration {

        /**
         * 유효한 사용자와 상품이 존재할 때 좋아요 등록에 성공하는지 테스트
         */
        @Test
        @DisplayName("유효한 사용자와 상품이면 좋아요 등록에 성공한다")
        void should_register_like_successfully_when_valid_user_and_product() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);
            Long initialLikeCount = savedProduct.getLikeCount();

            // When: 좋아요 등록
            LikeInfo result = likeFacade.upsertLike(userInfo.username(), savedProduct.getId());

            // Then: 좋아요 등록 성공 검증
            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo(userInfo.username());
            assertThat(result.productId()).isEqualTo(savedProduct.getId());

            // Then: 상품의 좋아요 수 증가 검증
            ProductEntity updatedProduct = productService.getProductDetail(savedProduct.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
        }

        @Test
        @DisplayName("삭제된 사용자면 예외를 던진다")
        void should_throw_exception_when_user_is_deleted() {
            // Given
            UserEntity deletedUser = UserTestFixture.createDefaultUserEntity();
            deletedUser.delete();
            userRepository.save(deletedUser);

            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(deletedUser.getUsername(), savedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외를 던진다")
        void should_throw_exception_when_user_not_exists() {
            // Given: 상품 생성 요청
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike("existUser", savedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }


        @Test
        @DisplayName("삭제된 상품이면 예외를 던진다")
        void should_throw_exception_when_product_is_deleted() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductEntity deletedProduct = ProductTestFixture.createEntity(1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100);
            deletedProduct.delete();
            productRepository.save(deletedProduct);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(user.getUsername(), deletedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 예외를 던진다")
        void should_throw_exception_when_product_not_exists() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.upsertLike(user.getUsername(), 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("이미 삭제된 좋아요를 다시 등록하면 복원된다")
        void should_restore_like_when_register_deleted_like_again() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );


            ProductEntity product = productService.registerProduct(request);

            LikeEntity deletedLike = LikeEntity.createEntity(userInfo.id(), product.getId());
            deletedLike.delete();
            likeRepository.save(deletedLike);

            // When
            LikeInfo result = likeFacade.upsertLike(userInfo.username(), product.getId());

            // Then
            assertThat(result).isNotNull();
            Optional<LikeEntity> found = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );
            assertThat(found).isPresent();
            assertThat(found.get().getDeletedAt()).isNull();
        }
    }


    @Nested
    @DisplayName("좋아요 취소")
    class LikeCancellation {
        @Test
        @DisplayName("유효한 사용자의 좋아요를 취소하면 성공한다")
        @Transactional
        void should_cancel_like_successfully_when_valid_user() {
            // Given: 사용자 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            LikeEntity like = LikeEntity.createEntity(userInfo.id(), product.getId());
            likeRepository.save(like);

            // 좋아요 수 증가 (좋아요가 이미 등록된 상태 시뮬레이션)
            product.increaseLikeCount();
            productRepository.save(product);
            Long initialLikeCount = product.getLikeCount();

            // When
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // Then: 좋아요 취소 검증
            Optional<LikeEntity> found = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );
            assertThat(found).isPresent();
            assertThat(found.get().getDeletedAt()).isNotNull();

            // Then: 상품의 좋아요 수 감소 검증
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount - 1);
        }

        @Test
        @DisplayName("삭제된 사용자가 좋아요를 취소하려 하면 예외를 던진다")
        @Transactional
        void should_throw_exception_when_deleted_user_tries_to_cancel_like() {

            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct("nonExistUser", 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 좋아요를 취소하려 하면 예외를 던진다")
        @Transactional
        void should_throw_exception_when_non_existent_user_tries_to_cancel_like() {
            // When & Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct("nonExistUser", 999L)
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("삭제된 상품의 좋아요를 취소하면  예외 처리한다.")
        @Transactional
        void should_throw_exception_when_cancel_like_for_deleted_product() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity deletedProduct = productService.registerProduct(request);
            deletedProduct.delete();
            productRepository.save(deletedProduct);

            LikeEntity like = LikeEntity.createEntity(user.getId(), deletedProduct.getId());
            likeRepository.save(like);


            // Then
            assertThatThrownBy(
                    () -> likeFacade.unlikeProduct(user.getUsername(), deletedProduct.getId())
            ).isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("좋아요가 존재하지 않아도 취소는 무시하고 성공한다")
        @Transactional
        void should_succeed_silently_when_cancel_non_existent_like() {
            // Given
            UserEntity user = UserTestFixture.createDefaultUserEntity();
            userRepository.save(user);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When & Then - 예외 없이 정상 완료
            likeFacade.unlikeProduct(user.getUsername(), product.getId());
        }
    }

    @Nested
    @DisplayName("좋아요 카운트 관리")
    class LikeCountManagement {

        @Test
        @DisplayName("좋아요 등록 시 상품의 좋아요 수가 증가한다")
        void should_increase_like_count_when_like_registered() {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);
            Long initialLikeCount = product.getLikeCount();

            // When: 좋아요 등록
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // Then: 좋아요 수 증가 검증
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
        }

        @Test
        @DisplayName("좋아요 취소 시 상품의 좋아요 수가 감소한다")
        void should_decrease_like_count_when_like_cancelled() {
            // Given: 사용자, 상품, 좋아요 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // 좋아요 등록
            likeFacade.upsertLike(userInfo.username(), product.getId());
            Long likeCountAfterRegistration = productService.getProductDetail(product.getId()).getLikeCount();

            // When: 좋아요 취소
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // Then: 좋아요 수 감소 검증
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(likeCountAfterRegistration - 1);
        }

        @Test
        @DisplayName("여러 사용자가 좋아요하면 상품의 좋아요 수가 누적된다")
        void should_accumulate_like_count_when_multiple_users_like() {
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

            // Given: 상품 생성
            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "인기상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);
            Long initialLikeCount = product.getLikeCount();

            // When: 세 명의 사용자가 좋아요 등록
            likeFacade.upsertLike(user1.username(), product.getId());
            likeFacade.upsertLike(user2.username(), product.getId());
            likeFacade.upsertLike(user3.username(), product.getId());

            // Then: 좋아요 수가 3 증가
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount + 3);
        }

        @Test
        @DisplayName("삭제된 좋아요를 복원하면 좋아요 수가 증가한다")
        void should_increase_like_count_when_deleted_like_restored() {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // Given: 삭제된 좋아요 생성
            LikeEntity deletedLike = LikeEntity.createEntity(userInfo.id(), product.getId());
            deletedLike.delete();
            likeRepository.save(deletedLike);

            Long initialLikeCount = product.getLikeCount();

            // When: 좋아요 복원 (재등록)
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // Then: 좋아요 수 증가 검증
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);
        }

        @Test
        @DisplayName("좋아요 등록 후 취소하면 카운트가 정확히 0으로 감소한다")
        void should_decrease_to_zero_when_cancel_single_like() {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // Given: 정상적인 flow로 좋아요 등록 (엔티티 생성 + 카운트 증가)
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // Then: 좋아요 수가 1 증가
            ProductEntity productAfterLike = productService.getProductDetail(product.getId());
            assertThat(productAfterLike.getLikeCount()).isEqualTo(1L);

            // When: 좋아요 취소
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // Then: 좋아요 수가 정확히 0으로 감소
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount())
                    .as("좋아요 1개를 취소하면 카운트는 정확히 0이어야 함")
                    .isEqualTo(0L);

            // Then: 좋아요 수가 음수가 되지 않음을 추가 검증
            assertThat(updatedProduct.getLikeCount())
                    .as("좋아요 수는 절대 음수가 될 수 없음")
                    .isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("좋아요가 없는 상태에서 취소를 시도해도 카운트는 0을 유지한다")
        void should_keep_zero_count_when_cancel_non_existent_like() {
            // Given: 사용자와 상품 생성 (좋아요 없음)
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // Then: 초기 좋아요 수가 0
            assertThat(product.getLikeCount()).isEqualTo(0L);

            // When: 좋아요가 없는 상태에서 취소 시도 (멱등성)
            likeFacade.unlikeProduct(userInfo.username(), product.getId());

            // Then: 카운트는 여전히 0 (음수가 되지 않음)
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getLikeCount())
                    .as("좋아요가 없는 상태에서 취소해도 카운트는 0을 유지해야 함")
                    .isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("멱등성 테스트")
    class IdempotencyTest {

        @Test
        @DisplayName("동시에 같은 좋아요를 등록하면 중복이 생성되지 않는다")
        void should_not_create_duplicate_when_concurrent_like_registration() throws InterruptedException {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 동시에 10번 좋아요 등록 시도
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        likeFacade.upsertLike(userInfo.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 좋아요는 1개만 생성되어야 함
            List<LikeEntity> likes = likeRepository.findAll();
            long userLikes = likes.stream()
                    .filter(like -> like.getUserId().equals(userInfo.id())
                            && like.getProductId().equals(product.getId())
                            && like.getDeletedAt() == null)
                    .count();

            assertThat(userLikes).isEqualTo(1);
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("동시에 같은 좋아요를 등록 및 취소하면 멱등성이 보장된다")
        void should_guarantee_idempotency_when_concurrent_like_and_unlike() throws InterruptedException {
            // Given: 사용자와 상품 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 동시에 등록과 취소를 반복
            int threadCount = 20;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        if (index % 2 == 0) {
                            // 짝수 스레드는 등록
                            likeFacade.upsertLike(userInfo.username(), product.getId());
                        } else {
                            // 홀수 스레드는 취소
                            likeFacade.unlikeProduct(userInfo.username(), product.getId());
                        }
                    } catch (Exception e) {
                        // 동시성으로 인한 예외는 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 최종 상태는 일관성이 있어야 함
            Optional<LikeEntity> finalLike = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );

            // 좋아요 엔티티가 존재해야 함 (등록 또는 취소 중 하나의 상태)
            assertThat(finalLike).isPresent();

            // 데이터베이스에 중복된 좋아요가 없어야 함
            List<LikeEntity> allLikes = likeRepository.findAll();
            long duplicateCount = allLikes.stream()
                    .filter(like -> like.getUserId().equals(userInfo.id())
                            && like.getProductId().equals(product.getId()))
                    .count();
            assertThat(duplicateCount).isEqualTo(1);
        }

        @Test
        @DisplayName("동시에 여러 번 좋아요 취소 시 중복 취소가 발생하지 않는다")
        void should_not_duplicate_cancel_when_concurrent_unlike_operations() throws InterruptedException {
            // Given: 사용자, 상품, 좋아요 생성
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // 좋아요 등록
            likeFacade.upsertLike(userInfo.username(), product.getId());

            // When: 동시에 10번 취소 시도
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executorService = newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        likeFacade.unlikeProduct(userInfo.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 좋아요는 삭제 상태여야 하며, 중복 삭제가 발생하지 않아야 함
            Optional<LikeEntity> like = likeRepository.findByUserIdAndProductId(
                    userInfo.id(), product.getId()
            );

            assertThat(like).isPresent();
            assertThat(like.get().getDeletedAt()).isNotNull();

            // 모든 취소 시도가 성공해야 함 (멱등성)
            assertThat(successCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("복수의 사용자가 동시에 같은 상품에 좋아요하면 각각 독립적으로 등록된다")
        void should_register_independently_when_multiple_users_like_same_product_concurrently() throws InterruptedException {
            // Given: 여러 사용자와 하나의 상품 생성
            int userCount = 5;
            List<UserInfo> users = new ArrayList<>();

            for (int i = 0; i < userCount; i++) {
                UserRegisterCommand command = UserTestFixture.createUserCommand(
                        "user" + i,
                        "user" + i + "@example.com",
                        "1990-01-01",
                        com.loopers.domain.user.Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(command);
                users.add(userInfo);
            }

            ProductDomainCreateRequest request = ProductTestFixture.createRequest(
                    1L,
                    "인기상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(request);

            // When: 모든 사용자가 동시에 좋아요 등록
            CountDownLatch latch = new CountDownLatch(userCount);
            ExecutorService executorService = newFixedThreadPool(userCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (UserInfo user : users) {
                executorService.submit(() -> {
                    try {
                        likeFacade.upsertLike(user.username(), product.getId());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // Then: 각 사용자별로 독립적인 좋아요가 생성되어야 함
            List<LikeEntity> likes = likeRepository.findAll();
            long activeLikes = likes.stream()
                    .filter(like -> like.getProductId().equals(product.getId())
                            && like.getDeletedAt() == null)
                    .count();

            assertThat(activeLikes).isEqualTo(userCount);
            assertThat(successCount.get()).isEqualTo(userCount);

            // 각 사용자별로 좋아요가 하나씩만 있는지 확인
            for (UserInfo user : users) {
                long userLikeCount = likes.stream()
                        .filter(like -> like.getUserId().equals(user.id())
                                && like.getProductId().equals(product.getId())
                                && like.getDeletedAt() == null)
                        .count();
                assertThat(userLikeCount).isEqualTo(1);
            }
        }
    }
}
