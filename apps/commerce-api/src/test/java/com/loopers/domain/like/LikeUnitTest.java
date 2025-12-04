package com.loopers.domain.like;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.UserEntity;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;

/**
 * LikeService의 단위 테스트 클래스
 * 좋아요 등록, 취소 기능을 테스트하며, 중복 방지 및 멱등성 처리를 검증합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 단위 테스트")
class LikeUnitTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private LikeService likeService;

    /**
     * 좋아요 등록 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 등록")
    class LikeRegistration {

        @Test
        @DisplayName("좋아요 관계가 존재하지 않으면 신규 생성한다.")
        void createLikeWhenNotExist() {
            // Given: 좋아요 관계가 존재하지 않는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );

            LikeEntity newLike = LikeEntity.createEntity(user.getId(), product.getId());

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.empty());
            when(likeRepository.save(any(LikeEntity.class))).thenReturn(newLike);

            // When: 좋아요 등록 메서드 호출
            LikeResult likeResult = likeService.upsertLike(user, product);

            // Then: 신규 생성되었는지 검증 (MV 테이블은 별도 서비스에서 처리)
            assertNotNull(likeResult);
            verify(likeRepository, times(1)).save(any(LikeEntity.class));
            // ProductRepository의 incrementLikeCount는 더 이상 사용하지 않음
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계가 존재하면 복원한다.")
        void restoreDeletedLikeWhenExist() {
            // Given: 삭제된 좋아요 엔티티가 존재하는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );

            LikeEntity deletedLike = LikeEntity.createEntity(user.getId(), product.getId());
            deletedLike.delete(); // 삭제 상태로 설정

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.of(deletedLike));

            // When: 좋아요 등록 메서드 호출 (복원)
            LikeResult result = likeService.upsertLike(user, product);

            // Then: 복원되었는지 검증
            assertNotNull(result);
            assertNull(result.entity().getDeletedAt(), "복원 후 deletedAt이 null이어야 함");
            verify(likeRepository, never()).save(any(LikeEntity.class)); // 기존 엔티티 복원이므로 save 호출 안함
        }

        @Test
        @DisplayName("이미 존재하는 활성 좋아요 관계이면 중복 방지로 카운트를 증가시키지 않는다")
        void duplicateLikeDoesNotIncreaseCount() {
            // Given: 활성 상태의 좋아요 엔티티가 존재하는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            // 좋아요 수는 MV 테이블에서 관리하므로 ProductEntity에서 직접 설정하지 않음

            LikeEntity existingLike = LikeEntity.createEntity(user.getId(), product.getId());

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.of(existingLike));

            // When: 좋아요 등록 메서드 호출 (중복 시도)
            LikeResult likeResult = likeService.upsertLike(user, product);

            // Then: 기존 엔티티가 반환되고 중복 처리되었는지 검증
            assertEquals(existingLike, likeResult.entity(), "기존 엔티티가 반환되어야 함");
            // MV 테이블은 별도 서비스에서 처리하므로 ProductRepository 호출 검증하지 않음
        }
    }

    /**
     * 좋아요 취소 관련 테스트 케이스 그룹
     */
    @Nested
    @DisplayName("좋아요 취소")
    class LikeCancellation {

        @Test
        @DisplayName("존재하지 않는 좋아요 관계이면 취소를 무시한다")
        void ignoreCancelWhenNotExist() {
            // Given: 좋아요 관계가 존재하지 않는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.empty());

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(user, product);

            // Then: 변경사항 없음 (MV 테이블은 별도 서비스에서 처리)
            // ProductRepository 호출 검증하지 않음
        }

        @Test
        @DisplayName("존재하는 활성 좋아요 관계이면 취소한다.")
        void cancelActiveLike() {
            // Given: 활성 상태의 좋아요 엔티티가 존재하는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );

            LikeEntity activeLike = LikeEntity.createEntity(user.getId(), product.getId());

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.of(activeLike));

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(user, product);

            // Then: 엔티티가 삭제 상태로 변경되었는지 검증
            assertNotNull(activeLike.getDeletedAt(), "취소 후 deletedAt이 설정되어야 함");
        }

        @Test
        @DisplayName("이미 삭제된 좋아요 관계이면 취소를 무시한다")
        void ignoreCancelWhenAlreadyDeleted() {
            // Given: 이미 삭제된 좋아요 엔티티가 존재하는 상황 설정
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            ProductEntity product = ProductTestFixture.createEntity(
                    1L,
                    "테스트 상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );

            LikeEntity deletedLike = LikeEntity.createEntity(user.getId(), product.getId());
            deletedLike.delete(); // 이미 삭제 상태

            when(likeRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .thenReturn(Optional.of(deletedLike));

            // When: 좋아요 취소 메서드 호출
            likeService.unlikeProduct(user, product);

            // Then: 이미 삭제된 상태이므로 아무 작업도 수행되지 않음 (멱등성 보장)
            assertNotNull(deletedLike.getDeletedAt(), "이미 삭제된 상태 유지");
        }
    }
}
