package com.loopers.domain.point;

import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PointService 단위 테스트 - 차감/환불")
class PointServiceTest {

    private PointRepository pointRepository;
    private UserRepository userRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointRepository = mock(PointRepository.class);
        userRepository = mock(UserRepository.class);
        pointService = new PointService(pointRepository, userRepository);
    }

    @Nested
    @DisplayName("포인트 차감(consume)")
    class Consume {

        @Test
        @DisplayName("성공 - 보유 포인트에서 차감")
        void consume_success() {
            // given
            String userId = "user1";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(Point.create(userId, 1000L)));
            when(pointRepository.save(any(Point.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Point result = pointService.consume(userId, 300L);

            // then
            assertThat(result.getAmount()).isEqualTo(700L);
            verify(pointRepository).save(argThat(p -> p.getAmount().equals(700L)));
        }

        @Test
        @DisplayName("실패 - 보유 포인트 부족")
        void consume_insufficient() {
            // given
            String userId = "user2";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(Point.create(userId, 100L)));

            // when & then
            assertThatThrownBy(() -> pointService.consume(userId, 300L))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("포인트가 부족합니다.");
            verify(pointRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 0 이하 금액 차감")
        void consume_invalid_amount() {
            // given
            String userId = "user3";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(Point.create(userId, 1000L)));

            // when & then
            assertThatThrownBy(() -> pointService.consume(userId, 0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("차감할 포인트는 0보다 커야 합니다.");

            assertThatThrownBy(() -> pointService.consume(userId, -10L))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("차감할 포인트는 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void consume_user_not_found() {
            // given
            String userId = "unknown";
            when(userRepository.existsById(userId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> pointService.consume(userId, 100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("존재하지 않는 사용자입니다");
        }
    }

    @Nested
    @DisplayName("포인트 환불(refund)")
    class Refund {

        @Test
        @DisplayName("성공 - 보유 포인트에 추가")
        void refund_success() {
            // given
            String userId = "user4";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(Point.create(userId, 100L)));
            when(pointRepository.save(any(Point.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Point result = pointService.refund(userId, 50L);

            // then
            assertThat(result.getAmount()).isEqualTo(150L);
            verify(pointRepository).save(argThat(p -> p.getAmount().equals(150L)));
        }

        @Test
        @DisplayName("실패 - 0 이하 금액 환불")
        void refund_invalid_amount() {
            // given
            String userId = "user5";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(pointRepository.findByUserId(userId)).thenReturn(Optional.of(Point.create(userId, 0L)));

            // when & then
            assertThatThrownBy(() -> pointService.refund(userId, 0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("추가할 포인트는 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void refund_user_not_found() {
            // given
            String userId = "unknown";
            when(userRepository.existsById(userId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> pointService.refund(userId, 100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("존재하지 않는 사용자입니다");
        }
    }
}
