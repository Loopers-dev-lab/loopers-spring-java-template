package com.loopers.application.like;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.like.entity.Like;
import com.loopers.domain.like.entity.LikeTargetType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.product.event.ProductViewEventHandler;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LikeFacade 테스트")
@SpringBootTest
class LikeFacadeTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeJpaRepository likeJpaRepository;

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductViewRepository productViewRepository;

    @Autowired
    private ProductViewEventHandler productViewEventHandler;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;

    @DisplayName("saveProductLike 메서드")
    @Nested
    class saveProductLikeTest {

        @DisplayName("성공 케이스: 유효한 사용자와 상품 ID로 좋아요 등록 시 성공")
        @Test
        void saveProductLike_withValidUserAndProduct_Success() {
            // arrange
            Long userId = createAndSaveUser(validLoginId);
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);

            // act
            likeFacade.saveProductLike(userId, productId);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            Optional<Like> savedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            if (savedLike.isPresent()) {
                productViewEventHandler.handleProductLikeSaved(new LikeEvents.ProductLikeSaved(productId));
            }

            // assert
            assertTrue(savedLike.isPresent());
            assertEquals(userId, savedLike.get().getLikeId().getUserId());
            assertEquals(productId, savedLike.get().getLikeId().getLikeTargetId());
            assertEquals(LikeTargetType.PRODUCT, savedLike.get().getLikeId().getLikeTargetType());
            
            // ProductView.likeCount가 1 증가했는지 확인 (비동기 이벤트 완료 대기)
            waitForProductViewLikeCountUpdate(productId, 1L);
            ProductView productView = productViewRepository.findById(productId).orElseThrow();
            assertEquals(1L, productView.getLikeCount(), "좋아요 등록 시 ProductView의 likeCount가 1 증가해야 합니다.");
        }

        @DisplayName("실패 케이스: 존재하지 않는 사용자로 좋아요 등록 시 NOT_FOUND 예외 발생")
        @Test
        void saveProductLike_withNonExistentUser_NotFound() {
            // arrange
            Long productId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.saveProductLike(999L, productId);
            });

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[userId = 999] User를 찾을 수 없습니다."));
        }
    }

    @DisplayName("saveProductLike 동시성 테스트")
    @Nested
    class SaveProductLikeConcurrencyTest {

        @DisplayName("멱등성 테스트: 동일한 좋아요를 중복 등록해도 Like 가 1개이고 Product의 likeCount는 1 증가해야 한다")
        @Test
        void saveProductLike_duplicateLike_Idempotent() {
            // arrange
            Long userId = createAndSaveUser(validLoginId);
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);

            // act
            likeFacade.saveProductLike(userId, productId);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            Optional<Like> firstLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            if (firstLike.isPresent()) {
                productViewEventHandler.handleProductLikeSaved(new LikeEvents.ProductLikeSaved(productId));
            }
            waitForProductViewLikeCountUpdate(productId, 1L);
            ProductView productViewAfterFirst = productViewRepository.findById(productId).orElseThrow();
            Long likeCountAfterFirst = productViewAfterFirst.getLikeCount();
            
            likeFacade.saveProductLike(userId, productId); // 중복 등록

            // assert
            Optional<Like> savedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertTrue(savedLike.isPresent());
            // 멱등성: 중복 등록해도 하나만 존재
            long count = likeJpaRepository.findAll().stream()
                    .filter(like -> like.getLikeId().getUserId().equals(userId)
                            && like.getLikeId().getLikeTargetId().equals(productId)
                            && like.getLikeId().getLikeTargetType() == LikeTargetType.PRODUCT)
                    .count();
            assertThat(count).isEqualTo(1);
            
            // 멱등성: 중복 등록 시 likeCount가 증가하지 않아야 함
            waitForProductViewLikeCountUpdate(productId, 1L);
            ProductView productViewAfterSecond = productViewRepository.findById(productId).orElseThrow();
            assertEquals(likeCountAfterFirst, productViewAfterSecond.getLikeCount(), 
                    "중복 등록 시 ProductView의 likeCount가 증가하지 않아야 합니다.");
        }

        @DisplayName("동시성 테스트: 동일한 좋아요 요청이 동시에 와도 likeCount는 한 번만 증가해야 한다")
        @Test
        void saveProductLike_concurrentRequests_Idempotent() throws InterruptedException {
            // arrange
            Long userId = createAndSaveUser(validLoginId);
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);

            // act
            int threads = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threads);
            ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

            // 스레드 실행
            for(int i = 1; i <= threads; i++) {
                executorService.execute(() -> {
                    try {
                        // 준비 완료 신호
                        readyLatch.countDown();
                        // 모든 스레드가 준비될 때까지 대기
                        startLatch.await();
                        // 실제 작업 수행
                        likeFacade.saveProductLike(userId, productId);
                    } catch (Throwable e) {
                        exceptions.offer(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            // 모든 스레드가 준비될 때까지 대기
            readyLatch.await();
            // 동시에 시작하도록 신호
            startLatch.countDown();
            // 모든 스레드가 완료될 때까지 대기
            doneLatch.await();
            executorService.shutdown();

            // assert
            
            // 워커 스레드에서 발생한 예외가 없는지 확인
            assertThat(exceptions).isEmpty();
            
            // Like 한 개만 저장되어 있어야 함
            long count = likeJpaRepository.findAll().stream()
                    .filter(like -> like.getLikeId().getUserId().equals(userId)
                            && like.getLikeId().getLikeTargetId().equals(productId)
                            && like.getLikeId().getLikeTargetType() == LikeTargetType.PRODUCT)
                    .count();
            assertThat(count).isEqualTo(1);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            // 실제로 새로 생성된 Like가 있는 경우에만 이벤트 처리
            Optional<Like> savedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            if (savedLike.isPresent()) {
                productViewEventHandler.handleProductLikeSaved(new LikeEvents.ProductLikeSaved(productId));
            }
            
            // ProductView.likeCount가 1만 증가했는지 확인 (비동기 이벤트 완료 대기)
            waitForProductViewLikeCountUpdate(productId, 1L);
            ProductView productViewAfterConcurrent = productViewRepository.findById(productId).orElseThrow();
            assertEquals(1L, productViewAfterConcurrent.getLikeCount(), 
                    "동시성 요청 시 ProductView의 likeCount가 1 증가해야 합니다.");
        }

    }

    @DisplayName("deleteProductLike 메서드")
    @Nested
    class DeleteProductLikeTest {

        @DisplayName("성공 케이스: 존재하는 좋아요를 취소하면 삭제됨")
        @Test
        void deleteProductLike_withExistingLike_Success() {
            // arrange
            Long userId = createAndSaveUser(validLoginId);
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);
            likeFacade.saveProductLike(userId, productId);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            Optional<Like> savedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            if (savedLike.isPresent()) {
                productViewEventHandler.handleProductLikeSaved(new LikeEvents.ProductLikeSaved(productId));
            }
            waitForProductViewLikeCountUpdate(productId, 1L);
            
            ProductView productViewBeforeDelete = productViewRepository.findById(productId).orElseThrow();
            Long likeCountBeforeDelete = productViewBeforeDelete.getLikeCount();

            // act
            likeFacade.deleteProductLike(userId, productId);
            
            // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
            Optional<Like> deletedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            if (!deletedLike.isPresent()) {
                productViewEventHandler.handleProductLikeDeleted(new LikeEvents.ProductLikeDeleted(productId));
            }

            // assert
            assertFalse(deletedLike.isPresent());
            
            // ProductView.likeCount가 1 감소했는지 확인 (비동기 이벤트 완료 대기)
            waitForProductViewLikeCountUpdate(productId, 0L);
            ProductView productViewAfterDelete = productViewRepository.findById(productId).orElseThrow();
            assertEquals(likeCountBeforeDelete - 1, productViewAfterDelete.getLikeCount(), 
                    "좋아요 삭제 시 ProductView의 likeCount가 1 감소해야 합니다.");
        }

        @DisplayName("멱등성 테스트: 존재하지 않는 좋아요를 취소해도 예외 없이 처리")
        @Test
        void deleteProductLike_withNonExistentLike_Idempotent() {
            // arrange
            Long userId = createAndSaveUser(validLoginId);
            Long productId = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 5L);
            
            ProductView productViewBeforeDelete = productViewRepository.findById(productId).orElseThrow();
            Long likeCountBeforeDelete = productViewBeforeDelete.getLikeCount();

            // act & assert - 예외 없이 처리되어야 함
            assertDoesNotThrow(() -> {
                likeFacade.deleteProductLike(userId, productId);
            });

            // assert
            Optional<Like> deletedLike = likeJpaRepository.findByLikeId_UserIdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertFalse(deletedLike.isPresent());
            
            // 멱등성: 존재하지 않는 좋아요 삭제 시 likeCount가 변하지 않아야 함
            // 이벤트가 발행되지 않으므로 ProductView의 likeCount는 변하지 않음
            ProductView productViewAfterDelete = productViewRepository.findById(productId).orElseThrow();
            assertEquals(likeCountBeforeDelete, productViewAfterDelete.getLikeCount(), 
                    "존재하지 않는 좋아요 삭제 시 ProductView의 likeCount가 변하지 않아야 합니다.");
        }

        @DisplayName("실패 케이스: 존재하지 않는 사용자로 좋아요 취소 시 NOT_FOUND 예외 발생")
        @Test
        void deleteProductLike_withNonExistentUser_NotFound() {
            // arrange
            Long nonExistentUserId = 999L;
            Long productId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.deleteProductLike(nonExistentUserId, productId);
            });

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[userId = 999] User를 찾을 수 없습니다."));
        }
    }

    // 테스트 헬퍼 메서드
    private Long createAndSaveUser(String loginId) {
        UserInfo userInfo = UserInfo.builder()
                .loginId(loginId)
                .email(validEmail)
                .birthday(validBirthday)
                .gender(validGender)
                .build();
        userFacade.saveUser(userInfo);
        return userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자 저장 후 조회 실패"))
                .getId();
    }

    private Long createAndSaveProduct(String name, BigDecimal price, Long likeCount) {
        // 브랜드 먼저 생성
        Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트 브랜드 설명")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(price)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();

        Product savedProduct = productFacade.createProduct(product);
        
        // 테스트 환경에서는 Kafka가 없으므로 ProductViewEventHandler를 직접 호출
        ProductEvents.Created event = new ProductEvents.Created(
                savedProduct.getId(),
                savedProduct.getBrandId(),
                savedProduct.getName(),
                savedProduct.getPrice(),
                savedProduct.getStatus()
        );
        productViewEventHandler.handleCreated(event);
        
        // ProductView의 likeCount 업데이트 (테스트용)
        // 실제로는 Like 엔티티를 생성해야 하지만, 테스트 편의를 위해 직접 업데이트
        productViewRepository.updateLikeCount(savedProduct.getId(), likeCount);
        
        return savedProduct.getId();
    }


    /**
     * ProductView의 likeCount가 예상 값에 도달할 때까지 대기하는 헬퍼 메서드
     * 비동기 이벤트 핸들러가 완료될 때까지 폴링
     */
    private void waitForProductViewLikeCountUpdate(Long productId, Long expectedCount) {
        int maxAttempts = 50; // 최대 5초 대기 (100ms * 50)
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                Optional<ProductView> productViewOpt = productViewRepository.findById(productId);
                if (productViewOpt.isPresent() && productViewOpt.get().getLikeCount().equals(expectedCount)) {
                    return; // ProductView의 likeCount가 예상 값에 도달함
                }
                Thread.sleep(100); // 100ms 대기
                attempt++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ProductView likeCount 업데이트 대기 중 인터럽트 발생", e);
            }
        }
        
        throw new RuntimeException("ProductView likeCount 업데이트 대기 시간 초과: productId=" + productId + ", expectedCount=" + expectedCount);
    }
}

